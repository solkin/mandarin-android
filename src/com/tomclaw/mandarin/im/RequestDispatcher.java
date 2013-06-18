package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;

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
    private final ContentObserver requestObserver;
    private final ContentObserver accountObserver;
    private final ContentResolver contentResolver;
    private Thread dispatcherThread;
    private final Object sync;

    public RequestDispatcher(Context context) {
        // Creating observers.
        requestObserver = new RequestObserver();
        accountObserver = new AccountObserver();
        // Variables.
        contentResolver = context.getContentResolver();
        // Initializing thread.
        sync = new Object();
        dispatcherThread = new DispatcherThread();
        // Registering created observers.
        contentResolver.registerContentObserver(
                Settings.REQUEST_RESOLVER_URI, true, requestObserver);
        contentResolver.registerContentObserver(
                Settings.ACCOUNT_RESOLVER_URI, true, accountObserver);
        // Almost done. Starting.
        dispatcherThread.start();
    }

    private class DispatcherThread extends Thread {
        @Override
        public void run() {
            while (true) {
                synchronized (sync) {
                    Log.d(Settings.LOG_TAG, "Obtain requests. If exist.");
                    // Obtain requests. If exist.
                    Cursor cursor = contentResolver.query(Settings.REQUEST_RESOLVER_URI, null, null, null, null);
                    // Checking for at least one request in database.
                    if (cursor.moveToFirst()) {
                        do {
                            // Obtain necessary column index.
                            int rowColumnIndex = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
                            int classColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_CLASS);
                            int sessionColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_SESSION);
                            int persistentColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_PERSISTENT);
                            int accountColumnIndex = cursor.getColumnIndex(GlobalProvider.REQUEST_ACCOUNT);
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
                                    continue;
                                }
                            }

                            Log.d(Settings.LOG_TAG, "Request received: "
                                    + "class = " + cursor.getString(classColumnIndex) + "; "
                                    + "session = " + cursor.getString(sessionColumnIndex) + "; "
                                    + "persistent = " + cursor.getInt(persistentColumnIndex) + "; "
                                    + "account = " + cursor.getInt(accountColumnIndex) + "; "
                                    + "state = " + cursor.getInt(stateColumnIndex) + "; "
                                    + "bundle = " + cursor.getString(bundleColumnIndex) + "");

                            // Obtain account root and request class (type).
                            AccountRoot accountRoot = QueryHelper.getAccount(contentResolver, cursor.getInt(accountColumnIndex));
                            String requestClass = cursor.getString(classColumnIndex);

                            int requestResult;
                            try {
                                // Preparing request.
                                Request request = (Request) Class.forName(requestClass).newInstance();
                                requestResult = request.onRequest(accountRoot);
                            } catch (Throwable e) {
                                Log.d(Settings.LOG_TAG, "Exception while loading request class: " + requestClass);
                                requestResult = Request.REQUEST_DELETE;
                            }
                            // Checking for request result.
                            if (requestResult == Request.REQUEST_DELETE) {
                                // Result is delete-type.
                                Log.d(Settings.LOG_TAG, "Result is delete-type");
                                contentResolver.delete(Settings.REQUEST_RESOLVER_URI,
                                        GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                            } else {
                                // Updating this request.
                                Log.d(Settings.LOG_TAG, "Updating this request");
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(GlobalProvider.REQUEST_STATE, requestResult);
                                contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                                        GlobalProvider.ROW_AUTO_ID + "='" + requestDbId + "'", null);
                            }
                        } while (cursor.moveToNext());
                    }
                    try {
                        // Wait until notifying. Try it.
                        sync.wait();
                    } catch (InterruptedException ignored) {
                        // Notified.
                    }
                }
            }
        }
    }

    /**
     * Handle all requests table changes.
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
            Log.d(Settings.LOG_TAG, "RequestObserver: onChange [" + selfChange + "]");
            synchronized (sync) {
                sync.notify();
            }
        }
    }

    /**
     * Needs to control account set change.
     * If account was deleted - drop all associated requests.
     * If status changed to any online - check queue and send associated requests.
     */
    private class AccountObserver extends ContentObserver {
        /**
         * Creates a content observer.
         */
        public AccountObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(Settings.LOG_TAG, "AccountsObserver: onChange [selfChange = " + selfChange + "]");
            synchronized (sync) {
                sync.notify();
            }
        }
    }
}
