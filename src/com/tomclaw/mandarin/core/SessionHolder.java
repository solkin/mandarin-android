package com.tomclaw.mandarin.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.core.accounts.AccountAuthenticator;
import com.tomclaw.mandarin.im.AccountRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 2:10 AM
 * Store opened accounts and sessions
 */
public class SessionHolder {

    private final String ACCOUNT_DATA = "account_data";
    private final String ACCOUNT_TYPE = "account_type";

    private List<AccountRoot> accountRootList = new ArrayList<AccountRoot>();
    private final ContentResolver contentResolver;
    private final AccountManager accountManager;

    public SessionHolder(Context context) {
        accountManager = AccountManager.get(context);
        // Obtain content resolver to perform queries.
        contentResolver = context.getContentResolver();
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

    /**
     * Check out difference between current and specified accounts.
     * @param accounts
     */
    public void update(Account[] accounts) {
        // Clear and fill up with updated list.
        accountRootList.clear();
        for(Account account : accounts) {
            // Checking for account type.
            if(account.type.equals(AccountAuthenticator.ACCOUNT_TYPE)) {
                // Obtain account clone from account manager.
                AccountRoot accountRoot = getAccountRoot(account);
                accountRootList.add(accountRoot);
                // Query fot such account in database.
                // if(QueryHelper.updateAccount(contentResolver, accountRoot)) {
                    // Account was changed. Account must be offline now. Switch it.
                // }
            }
        }
    }

    public List<AccountRoot> getAccountsList() {
        return accountRootList;
    }

    public void addAccountRoot(AccountRoot accountRoot) {
        // This is the magic that adds the account to the Android Account Manager
        final Account account = new Account(accountRoot.getUserId(), AccountAuthenticator.ACCOUNT_TYPE);
        if(accountManager.addAccountExplicitly(account, accountRoot.getUserPassword(), getAccountData(accountRoot))) {
            // Query to add account.
            accountRootList.add(accountRoot);
        }
    }

    private Bundle getAccountData(AccountRoot accountRoot) {
        Bundle bundle = new Bundle();
        // Writing json data.
        Gson gson = new Gson();
        String accountData = gson.toJson(accountRoot);
        Log.d(Settings.LOG_TAG, "account data: " + accountData);
        bundle.putString(ACCOUNT_DATA, accountData);
        bundle.putString(ACCOUNT_TYPE, accountRoot.getClass().getName());
        return bundle;
    }

    private AccountRoot getAccountRoot(Account account) {
        // Identifying account from bundle.
        String accountData = accountManager.getUserData(account, ACCOUNT_DATA);
        String accountType = accountManager.getUserData(account, ACCOUNT_TYPE);
        Log.d(Settings.LOG_TAG, "account [" + accountType + "] data: " + accountData);
        Gson gson = new Gson();
        AccountRoot accountRoot = null;
        try {
            accountRoot = (AccountRoot) gson.fromJson(accountData, Class.forName(accountType));
        } catch (ClassNotFoundException e) {
            Log.d(Settings.LOG_TAG, "Unknown account class type: " + accountType);
        }
        return accountRoot;
    }
}
