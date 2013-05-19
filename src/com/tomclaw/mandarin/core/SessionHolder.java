package com.tomclaw.mandarin.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.tomclaw.mandarin.core.accounts.AccountAuthenticator;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 2:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class SessionHolder {

    private List<AccountRoot> accountRootList = new ArrayList<AccountRoot>();
    private final AccountManager accountManager;

    public SessionHolder(Context context) {
        accountManager = AccountManager.get(context);
        // Listener will invoke update when data changes in account manager.
        accountManager.addOnAccountsUpdatedListener(new OnAccountsUpdateListener() {
            @Override
            public void onAccountsUpdated(Account[] accounts) {
                Log.d(Settings.LOG_TAG, "SessionHolder: onAccountsUpdated");
                update(accounts);
            }
        }, null, false);
    }

    public void load() {
        Log.d(Settings.LOG_TAG, "SessionHolder: load");
        // Loading accounts from local storage.
        Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        update(accounts);
    }

    public void update(Account[] accounts) {
        // Clear and fill up with updated list.
        accountRootList.clear();
        for(Account account : accounts) {
            // Checking for account type.
            if(account.type.equals(AccountAuthenticator.ACCOUNT_TYPE)) {
                AccountRoot accountRoot = new IcqAccountRoot();
                accountRoot.setUserId(account.name);
                accountRoot.setUserNick(account.name);
                accountRootList.add(accountRoot);
            }
        }
    }

    public void save() {
        // Saving account to local storage.
    }

    public List<AccountRoot> getAccountsList() {
        return accountRootList;
    }

    public void addAccountRoot(AccountRoot accountRoot) {
        accountRootList.add(accountRoot);

        String accountType = AccountAuthenticator.ACCOUNT_TYPE;

        // This is the magic that adds the account to the Android Account Manager
        final Account account = new Account(accountRoot.getUserId(), accountType);
        accountManager.addAccountExplicitly(account, accountRoot.getUserPassword(), null);

        // Now we tell our caller, could be the Android Account Manager or even our own application
        // that the process was successful

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountRoot.getUserId());
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, accountType);
    }
}
