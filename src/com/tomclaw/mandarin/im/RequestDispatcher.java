package com.tomclaw.mandarin.im;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:27 PM
 */
public class RequestDispatcher {

    /** Variables **/
    private ContentObserver requestObserver;
    private ContentObserver accountObserver;

    public RequestDispatcher(Context context) {
        requestObserver = new RequestObserver();
        accountObserver = new AccountObserver();
        context.getContentResolver().registerContentObserver(
                Settings.REQUEST_RESOLVER_URI, true, requestObserver);
        context.getContentResolver().registerContentObserver(
                Settings.ACCOUNT_RESOLVER_URI, true, accountObserver);
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
            Log.d(Settings.LOG_TAG, "RequestObserver: onChange");
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
            Log.d(Settings.LOG_TAG, "AccountsObserver: onChange");
        }
    }
}
