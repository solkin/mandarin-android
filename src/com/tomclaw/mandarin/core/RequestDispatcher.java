package com.tomclaw.mandarin.core;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.text.TextUtils;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:27 PM
 */
public class RequestDispatcher {

    private static final long PENDING_REQUEST_DELAY = 3000;
    /**
     * Variables
     */
    private Service service;
    private final SessionHolder sessionHolder;
    private final ContentResolver contentResolver;
    private int requestType;
    private DispatcherRunnable runnable;
    private ThreadPoolExecutor executor;
    private RequestObserver requestObserver;

    private volatile String executingRequestTag;

    public RequestDispatcher(Service service, SessionHolder sessionHolder, int requestType) {
        this.service = service;
        // Session holder.
        this.sessionHolder = sessionHolder;
        // Request type.
        this.requestType = requestType;
        // Variables.
        contentResolver = service.getContentResolver();
        // Initializing executor and observer.
        initExecutor();
        runnable = new DispatcherRunnable();
        requestObserver = new RequestObserver();
    }

    private void initExecutor() {
        executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(2));
    }

    public void startObservation() {
        // Registering created observers.
        contentResolver.registerContentObserver(
                Settings.REQUEST_RESOLVER_URI, true, requestObserver);
        contentResolver.registerContentObserver(
                Settings.ACCOUNT_RESOLVER_URI, true, requestObserver);
        // Almost done. Starting.
        notifyQueue();
    }

    /**
     * Stops task with specified tag.
     *
     * @param tag - tag of the task needs to be stopped.
     */
    public boolean stopRequest(String tag) {
        // First of all, check that task is executing or in queue.
        if (TextUtils.equals(tag, executingRequestTag)) {
            // Task is executing this moment.
            // Interrupt thread as faster as it can be!
            // Task will receive interrupt exception.
            executor.shutdownNow();
            initExecutor();
            notifyQueue();
            return true;
        } else {
            // Huh... Task is only in scheduled queue.
            // We can simply mark is as delayed "REQUEST_LATER".
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_LATER);
            contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                    GlobalProvider.REQUEST_TAG + "='" + tag + "'", null);
            return false;
        }
    }

    private class DispatcherRunnable implements Runnable {

        @Override
        public void run() {
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.REQUEST_TYPE, requestType).and()
                    .columnNotEquals(GlobalProvider.REQUEST_STATE, Request.REQUEST_LATER);
            // Registering created observers.
            Cursor requestCursor = queryBuilder.query(contentResolver, Settings.REQUEST_RESOLVER_URI);
            // Check for we are ready to dispatch.
            if (requestCursor == null) {
                log("Something strange! Request or account cursor is null.");
                return;
            }
            try {
                dispatch(requestCursor);
            } finally {
                requestCursor.close();
            }
        }

        @SuppressWarnings("unchecked")
        private void dispatch(Cursor requestCursor) {
            // Yeah, we are ready.
            log("Dispatching requests.");
            int requests = 0;
            // Checking for at least one request in database.
            if (requestCursor.moveToFirst()) {
                requests = requestCursor.getCount();
                log("Found requests: " + requests);
                do {
                    log("Request...");
                    // Obtain necessary column index.
                    int rowColumnIndex = requestCursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
                    int classColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_CLASS);
                    int sessionColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_SESSION);
                    int persistentColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_PERSISTENT);
                    int accountColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_ACCOUNT_DB_ID);
                    int stateColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_STATE);
                    int bundleColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_BUNDLE);
                    int tagColumnIndex = requestCursor.getColumnIndex(GlobalProvider.REQUEST_TAG);
                    /**
                     * Если сессия совпадает, то постоянство задачи значения не имеет.
                     * Если задача непостоянная, сессия отличается, то задача отклоняется.
                     * Если задача постоянная, сессия отличается, то надо смотреть на статус:
                     *      В очереди:  задача отправляется, как и в случае "обработано".
                     *                  Обновляется ключ сессии приложения.
                     *      Обработано: задача может быть не доставленной до сервера, переотправить.
                     *                  Обновляется ключ сессии приложения.
                     *      Отправлено: задача была отправлена, не нуждается в отправке, хотя ответа явно
                     *                  не будет и задачу можно удалить.
                     */
                    // Obtain values.
                    int requestDbId = requestCursor.getInt(rowColumnIndex);
                    boolean isPersistent = requestCursor.getInt(persistentColumnIndex) == 1;
                    String requestAppSession = requestCursor.getString(sessionColumnIndex);
                    int requestState = requestCursor.getInt(stateColumnIndex);
                    // Checking for session is equals.
                    if (TextUtils.equals(requestAppSession, CoreService.getAppSession())) {
                        if (requestState != Request.REQUEST_PENDING) {
                            log("Processed request of current session.");
                            requests--;
                            continue;
                        }
                        log("Normal request and will be processed now.");
                    } else {
                        boolean isDecline = false;
                        boolean isBreak = false;
                        // Checking for query is persistent.
                        if (isPersistent) {
                            switch (requestState) {
                                case Request.REQUEST_PENDING: {
                                    // Persistent request, might be processed at anytime.
                                    log("Persistent request, might be processed at anytime.");
                                    break;
                                }
                                case Request.REQUEST_SENT: {
                                    // Request sent, processed by server,
                                    // but we have no answer. Decline.
                                    log("Request sent, processed by server, " +
                                            "but we have no answer. Decline.");
                                    isDecline = true;
                                    break;
                                }
                            }
                        } else {
                            // Decline request.
                            isDecline = true;
                            log("Another session and not persistent request.");
                        }
                        // Checking for request is obsolete and must be declined.
                        if (isDecline) {
                            contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                    GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                            requests--;
                            break;
                        }
                    }

                    String requestClass = requestCursor.getString(classColumnIndex);
                    int requestAccountDbId = requestCursor.getInt(accountColumnIndex);
                    String requestBundle = requestCursor.getString(bundleColumnIndex);
                    String requestTag = requestCursor.getString(tagColumnIndex);

                    log("Request received: "
                            + "class = " + requestClass + "; "
                            + "session = " + requestAppSession + "; "
                            + "persistent = " + isPersistent + "; "
                            + "account = " + requestAccountDbId + "; "
                            + "state = " + requestState + "; "
                            + "bundle = " + requestBundle + "");

                    int requestResult = Request.REQUEST_DELETE;
                    Request request = null;
                    try {
                        // Obtain account root and request class (type).
                        AccountRoot accountRoot = sessionHolder.getAccount(requestAccountDbId);
                        // Checking for account online.
                        if (accountRoot.isOffline()) {
                            // Account is offline now. Let's send this request later.
                            requests--;
                            continue;
                        }
                        // Preparing request.
                        request = (Request) GsonSingleton.getInstance().fromJson(
                                requestBundle, Class.forName(requestClass));
                        executingRequestTag = requestTag;
                        requestResult = request.onRequest(accountRoot, service);
                    } catch (AccountNotFoundException e) {
                        log("RequestDispatcher: account not found by request db id. " +
                                "Cancelling.");
                    } catch (Throwable ex) {
                        log("Exception while loading request class: " + requestClass, ex);
                    } finally {
                        executingRequestTag = null;
                    }
                    // Checking for request result.
                    if (requestResult == Request.REQUEST_DELETE) {
                        // Result is delete-type.
                        log("Result is delete-type");
                        contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                        requests--;
                    } else if (requestResult == Request.REQUEST_PENDING) {
                        // Request wasn't completed. We'll retry request a little bit later.
                        log("Request wasn't completed. We'll retry request a little bit later.");
                        break;
                    } else {
                        // Updating this request.
                        log("Updating this request");
                        String requestJson = GsonSingleton.getInstance().toJson(request);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(GlobalProvider.REQUEST_STATE, requestResult);
                        contentValues.put(GlobalProvider.REQUEST_BUNDLE, requestJson);
                        contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                                GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                    }
                } while (requestCursor.moveToNext());
            }
            log("Dispatching completed, pending requests: " + requests);
            if (requests > 0) {
                // Pending guarantee dispatching after delay.
                log("Pending guarantee dispatching after delay");
                backgroundQueueNotify(PENDING_REQUEST_DELAY);
            }
        }
    }

    private void log(String message) {
        Logger.log("rd[" + requestType + "]: " + message);
    }

    private void log(String message, Throwable exception) {
        Logger.log("rd[" + requestType + "]: " + message, exception);
    }

    public void notifyQueue() {
        try {
            executor.submit(runnable);
            log("Queue notification accepted.");
        } catch (RejectedExecutionException ignored) {
            // All right, this is useless task.
            log("Queue notification received, but we already have notification.");
        }
    }

    public void backgroundQueueNotify() {
        backgroundQueueNotify(0);
    }

    public void backgroundQueueNotify(final long delay) {
        // Strange thread working model. Need to be rewritten.
        new Thread() {
            public void run() {
                if(delay > 0) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ignored) {
                    }
                }
                notifyQueue();
            }
        }.start();
    }

    private class RequestObserver extends ContentObserver {

        /**
         * Creates a content observer.
         */
        public RequestObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Logger.log("RequestDispatcher: onChange [selfChange = " + selfChange + "]");
            notifyQueue();
        }
    }
}
