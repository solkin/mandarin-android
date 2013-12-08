package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:27 PM
 */
public class RequestDispatcher {

    /**
     * Variables
     */
    private final SessionHolder sessionHolder;
    private final ContentResolver contentResolver;
    private final ContentObserver requestObserver;
    private Thread dispatcherThread;
    private final Object sync;
    private Gson gson;
    private int requestType;

    public RequestDispatcher(Context context, SessionHolder sessionHolder, int requestType) {
        // Session holder.
        this.sessionHolder = sessionHolder;
        // Request type.
        this.requestType = requestType;
        // Variables.
        contentResolver = context.getContentResolver();
        // Creating observers.
        requestObserver = new RequestObserver();
        // Initializing thread.
        sync = new Object();
        gson = new Gson();
        dispatcherThread = new DispatcherThread();
    }

    public void startObservation() {
        // Almost done. Starting.
        dispatcherThread.setPriority(Thread.MIN_PRIORITY);
        dispatcherThread.start();
    }

    private class DispatcherThread extends Thread {

        @Override
        public void run() {
            Cursor cursor;
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.REQUEST_TYPE, requestType);
            do {
                // Registering created observers.
                cursor = queryBuilder.query(contentResolver, Settings.REQUEST_RESOLVER_URI);
                cursor.registerContentObserver(requestObserver);
                /**
                 * Needs to control account set change.
                 * If account was deleted - drop all associated requests.
                 * If status changed to any online - check queue and send associated requests.
                 */
                contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null, null, null, null)
                    .registerContentObserver(requestObserver);
            } while (dispatch(cursor));
        }

        @SuppressWarnings("unchecked")
        private boolean dispatch(Cursor cursor) {
            synchronized (sync) {
                Log.d(Settings.LOG_TAG, "Dispatching requests.");
                // Checking for at least one request in database.
                if (cursor.moveToFirst()) {
                    do {
                        Log.d(Settings.LOG_TAG, "Request...");
                        // Obtain necessary column index.
                        int rowColumnIndex = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
                        int classColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_CLASS);
                        int sessionColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_SESSION);
                        int persistentColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_PERSISTENT);
                        int accountColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_ACCOUNT_DB_ID);
                        int stateColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_STATE);
                        int bundleColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_BUNDLE);
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
                        int requestDbId = cursor.getInt(rowColumnIndex);
                        boolean isPersistent = cursor.getInt(persistentColumnIndex) == 1;
                        String requestAppSession = cursor.getString(sessionColumnIndex);
                        int requestState = cursor.getInt(stateColumnIndex);
                        // Checking for session is equals.
                        if (requestAppSession.equals(CoreService.getAppSession())) {
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
                                        // Request might be sent again, but we need to update request
                                        // application session to escape this clause anymore in this session.
                                        Log.d(Settings.LOG_TAG, "Request might be sent again, but we need to " +
                                                "update request application session to escape this clause " +
                                                "anymore in this session.");
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put(GlobalProvider.REQUEST_SESSION, CoreService.getAppSession());
                                        contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                                                GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                                        isBreak = true;
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
                            // Checking for content was changed.
                            if(isBreak) {
                                // We'll receive change event from observer soon.
                                break;
                            }
                            // Checking for request is obsolete and must be declined.
                            if (isDecline) {
                                contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                        GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                                break;
                            }
                        }

                        String requestClass = cursor.getString(classColumnIndex);
                        int requestAccountDbId =  cursor.getInt(accountColumnIndex);
                        String requestBundle =  cursor.getString(bundleColumnIndex);

                        Log.d(Settings.LOG_TAG, "Request received: "
                                + "class = " + requestClass + "; "
                                + "session = " + requestAppSession + "; "
                                + "persistent = " + isPersistent + "; "
                                + "account = " + requestAccountDbId + "; "
                                + "state = " + requestState + "; "
                                + "bundle = " + requestBundle + "");

                        int requestResult = Request.REQUEST_DELETE;
                        try {
                            // Obtain account root and request class (type).
                            AccountRoot accountRoot = sessionHolder.getAccount(requestAccountDbId);
                            // Checking for account online.
                            if(accountRoot.getStatusIndex() == StatusUtil.STATUS_OFFLINE) {
                                // Account is offline now. Let's send this request later.
                                continue;
                            }
                            // Preparing request.
                            Request request = (Request) gson.fromJson(
                                    requestBundle, Class.forName(requestClass));
                            requestResult = request.onRequest(accountRoot);
                        } catch (AccountNotFoundException e) {
                            Log.d(Settings.LOG_TAG, "RequestDispatcher: account not found by request db id. " +
                                    "Cancelling.");
                        } catch (Throwable e) {
                            Log.d(Settings.LOG_TAG, "Exception while loading request class: " + requestClass);
                            e.printStackTrace();
                        }
                        // Checking for request result.
                        if (requestResult == Request.REQUEST_DELETE) {
                            // Result is delete-type.
                            Log.d(Settings.LOG_TAG, "Result is delete-type");
                            contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                    GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                        } else if(requestResult == Request.REQUEST_PENDING) {
                            // Request wasn't completed. We'll retry request a little bit later.
                            Log.d(Settings.LOG_TAG, "Request wasn't completed. We'll retry request a little bit later.");
                            continue;
                        } else {
                            // Updating this request.
                            Log.d(Settings.LOG_TAG, "Updating this request");
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(GlobalProvider.REQUEST_STATE, requestResult);
                            contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                                    GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                        }
                        // Breaking. We'll receive change event from observer.
                        break;
                    } while (cursor.moveToNext());
                }
                try {
                    // Wait until notifying. Try it.
                    sync.wait();
                } catch (InterruptedException ignored) {
                    // Notified.
                }
            }
            cursor.close();
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
            super.onChange(selfChange);
            synchronized (sync) {
                sync.notify();
            }
        }
    }
}
