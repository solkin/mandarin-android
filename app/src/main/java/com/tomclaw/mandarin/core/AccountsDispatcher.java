package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;

import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Igor on 16.05.2015.
 */
public class AccountsDispatcher {

    private Context context;
    private final ContentResolver contentResolver;
    private final AccountsObserver accountsObserver;
    private SessionHolder sessionHolder;
    private boolean hasActiveAccounts;

    public AccountsDispatcher(Context context, SessionHolder sessionHolder) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.accountsObserver = new AccountsObserver();
        this.sessionHolder = sessionHolder;
    }

    public void startObservation() {
        hasActiveAccounts = sessionHolder.hasActiveAccounts();
        // Registering created observers.
        contentResolver.registerContentObserver(
                Settings.ACCOUNT_RESOLVER_URI, true, accountsObserver);
        accountsObserver.onChange(true);
    }

    public void stopObservation() {
        contentResolver.unregisterContentObserver(accountsObserver);
    }

    private class AccountsObserver extends ContentObserver {

        /**
         * Creates a content observer.
         */
        public AccountsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Logger.log("AccountsObserver: onChange [selfChange = " + selfChange + "]");
            boolean activeAccountsBefore = hasActiveAccounts;
            hasActiveAccounts = sessionHolder.hasActiveAccounts();
            // Checking for connected conditions.
            if (!activeAccountsBefore && hasActiveAccounts) {
                Logger.log("AccountsObserver: account was connected. We must notify core service.");
                // Some account has connecting or connected.
                Intent serviceIntent = new Intent(context, CoreService.class)
                        .putExtra(CoreService.EXTRA_ON_CONNECTED_EVENT, true);
                CoreService.startCoreService(context, serviceIntent);
            }
            // Checking for all accounts are now disconnected.
            if (activeAccountsBefore && !hasActiveAccounts) {
                Logger.log("AccountsObserver: accounts was disconnected. We must notify core service.");
                // All accounts are now disconnected.
                Intent serviceIntent = new Intent(context, CoreService.class)
                        .putExtra(CoreService.EXTRA_ON_DISCONNECTED_EVENT, true);
                CoreService.startCoreService(context, serviceIntent);
            }
        }
    }
}
