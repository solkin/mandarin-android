package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
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

    private final List<AccountRoot> accountRootList;
    private final Context context;
    private final ContentResolver contentResolver;
    public NotificationHelper notificationHelper;

    public SessionHolder(Context context, NotificationHelper notificationHelper) {
        this.context = context;
        // Obtain content resolver to perform queries.
        contentResolver = context.getContentResolver();
        accountRootList = new ArrayList<AccountRoot>();
        this.notificationHelper = notificationHelper;
    }

    public void load() {
        // Loading accounts from database.
        QueryHelper.getAccounts(context, accountRootList);
        for(AccountRoot accountRoot : accountRootList){
            accountRoot.setNotificationHelper(notificationHelper);
        }
    }

    public void updateAccountRoot(AccountRoot accountRoot) {
        // Attempting to update account.
        if (QueryHelper.updateAccount(context, accountRoot)) {
            // Account was created.
            accountRootList.add(accountRoot);
        }
    }

    public boolean removeAccountRoot(String accountType, String userId) {
        for (AccountRoot accountRoot : accountRootList) {
            // Checking for account type and user id.
            if (accountRoot.getAccountType().equals(accountType)
                    && accountRoot.getUserId().equals(userId)) {
                // Disconnect first of all.
                accountRoot.disconnect();
                // Now we ready to remove this account.
                accountRootList.remove(accountRoot);
                break;
            }
        }
        // Trying to remove all data from database, associated with this account.
        return QueryHelper.removeAccount(contentResolver, accountType, userId);
    }

    public void updateAccountStatus(String accountType, String userId, int statusIndex) {
        for (AccountRoot accountRoot : accountRootList) {
            // Checking for account type and user id.
            if (accountRoot.getAccountType().equals(accountType)
                    && accountRoot.getUserId().equals(userId)) {
                // Changing status.
                accountRoot.setStatus(statusIndex);
                return;
            }
        }
    }

    public List<AccountRoot> getAccountsList() {
        return accountRootList;
    }

    public AccountRoot getAccount(int accountDbId) throws AccountNotFoundException {
        for (AccountRoot accountRoot : accountRootList) {
            // Checking for account db id equals.
            if (accountRoot.getAccountDbId() == accountDbId) {
                return accountRoot;
            }
        }
        throw new AccountNotFoundException();
    }
}
