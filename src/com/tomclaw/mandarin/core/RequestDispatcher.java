package com.tomclaw.mandarin.core;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.QueryBuilder;

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
    private final ContentObserver requestObserver;
    private Thread dispatcherThread;
    private final Object sync;
    private int requestType;

    private volatile String executingRequestTag;

    public RequestDispatcher(Service service, SessionHolder sessionHolder, int requestType) {
        this.service = service;
        // Session holder.
        this.sessionHolder = sessionHolder;
        // Request type.
        this.requestType = requestType;
        // Variables.
        contentResolver = service.getContentResolver();
        // Creating observers.
        requestObserver = new RequestObserver();
        // Initializing thread.
        sync = new Object();
        dispatcherThread = new DispatcherThread();
    }

    public void startObservation() {
        // Almost done. Starting.
        dispatcherThread.setPriority(Thread.MIN_PRIORITY);
        dispatcherThread.start();
    }

    /**
     * Stops task with specified tag.
     * @param tag - tag of the task needs to be stopped.
     */
    public boolean stopRequest(String tag) {
        // First of all, check that task is executing or in queue.
        if(TextUtils.equals(tag, executingRequestTag)) {
            // Task is executing this moment.
            // Interrupt thread as faster as it can be!
            // Task will receive interrupt exception.
            dispatcherThread.interrupt();
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

    private class DispatcherThread extends Thread {

        @Override
        public void run() {
            Cursor requestCursor;
            Cursor accountCursor;
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.REQUEST_TYPE, requestType)
                .and().columnNotEquals(GlobalProvider.REQUEST_STATE, Request.REQUEST_LATER);
            do {
                // Registering created observers.
                requestCursor = queryBuilder.query(contentResolver, Settings.REQUEST_RESOLVER_URI);
                requestCursor.registerContentObserver(requestObserver);
                /**
                 * Needs to control account set change.
                 * If account was deleted - drop all associated requests.
                 * If status changed to any online - check queue and send associated requests.
                 */
                accountCursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
                if (accountCursor != null) {
                    accountCursor.registerContentObserver(requestObserver);
                }
            } while (dispatch(requestCursor, accountCursor));
        }

        @SuppressWarnings("unchecked")
        private boolean dispatch(Cursor requestCursor, Cursor accountCursor) {
            synchronized (sync) {
                Log.d(Settings.LOG_TAG, "Dispatching requests.");
                // Checking for at least one request in database.
                if (requestCursor.moveToFirst()) {
                    do {
                        Log.d(Settings.LOG_TAG, "Request...");
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
                                Log.d(Settings.LOG_TAG, "Processed request of current session.");
                                continue;
                            }
                            Log.d(Settings.LOG_TAG, "Normal request and will be processed now.");
                        } else {
                            boolean isDecline = false;
                            boolean isBreak = false;
                            // Checking for query is persistent.
                            if (isPersistent) {
                                switch (requestState) {
                                    case Request.REQUEST_PENDING: {
                                        // Persistent request, might be processed at anytime.
                                        Log.d(Settings.LOG_TAG, "Persistent request, might be processed at anytime.");
                                        break;
                                    }
                                    case Request.REQUEST_SENT: {
                                        // Request sent, processed by server,
                                        // but we have no answer. Decline.
                                        Log.d(Settings.LOG_TAG, "Request sent, processed by server, " +
                                                "but we have no answer. Decline.");
                                        isDecline = true;
                                        break;
                                    }
                                }
                            } else {
                                // Decline request.
                                isDecline = true;
                                Log.d(Settings.LOG_TAG, "Another session and not persistent request.");
                            }
                            // Checking for request is obsolete and must be declined.
                            if (isDecline) {
                                contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                        GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                                break;
                            }
                        }

                        String requestClass = requestCursor.getString(classColumnIndex);
                        int requestAccountDbId = requestCursor.getInt(accountColumnIndex);
                        String requestBundle = requestCursor.getString(bundleColumnIndex);
                        String requestTag = requestCursor.getString(tagColumnIndex);

                        Log.d(Settings.LOG_TAG, "Request received: "
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
                                continue;
                            }
                            // Preparing request.
                            request = (Request) GsonSingleton.getInstance().fromJson(
                                    requestBundle, Class.forName(requestClass));
                            executingRequestTag = requestTag;
                            requestResult = request.onRequest(accountRoot, service);
                        } catch (AccountNotFoundException e) {
                            Log.d(Settings.LOG_TAG, "RequestDispatcher: account not found by request db id. " +
                                    "Cancelling.");
                        } catch (Throwable ex) {
                            Log.d(Settings.LOG_TAG, "Exception while loading request class: " + requestClass, ex);
                        } finally {
                            executingRequestTag = null;
                        }
                        // Checking for request result.
                        if (requestResult == Request.REQUEST_DELETE) {
                            // Result is delete-type.
                            Log.d(Settings.LOG_TAG, "Result is delete-type");
                            contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                    GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                        } else if (requestResult == Request.REQUEST_PENDING) {
                            // Request wasn't completed. We'll retry request a little bit later.
                            Log.d(Settings.LOG_TAG, "Request wasn't completed. We'll retry request a little bit later.");
                            break;
                        } else {
                            // Updating this request.
                            Log.d(Settings.LOG_TAG, "Updating this request");
                            String requestJson = GsonSingleton.getInstance().toJson(request);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(GlobalProvider.REQUEST_STATE, requestResult);
                            contentValues.put(GlobalProvider.REQUEST_BUNDLE, requestJson);
                            contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                                    GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                        }
                        // Breaking. We'll receive change event from observer.
                        break;
                    } while (requestCursor.moveToNext());
                }
                try {
                    if(requestCursor.getCount() > 0) {
                        // Wait for specified daley or until notifying.
                        Log.d(Settings.LOG_TAG, "Wait for specified delay or until notifying");
                        sync.wait(PENDING_REQUEST_DELAY);
                    } else {
                        // Wait until notifying. Try it.
                        Log.d(Settings.LOG_TAG, "Wait until notifying");
                        sync.wait();
                    }
                } catch (InterruptedException ignored) {
                    // Notified.
                }
            }
            requestCursor.close();
            accountCursor.close();
            requestCursor.unregisterContentObserver(requestObserver);
            return true;
        }
    }

    /**
     * Handle all requests table and accounts changes.
     */
    private class RequestObserver extends ContentObserver {
        /**
         * Creates a content observer.
         */
        public RequestObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            synchronized (sync) {
                sync.notify();
            }
        }
    }

    public void notifyQueue() {
        synchronized (sync) {
            sync.notify();
        }
    }
}
