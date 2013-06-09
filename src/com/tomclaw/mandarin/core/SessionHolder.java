package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
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
    private final ContentResolver contentResolver;

    public SessionHolder(Context context) {
        // Obtain content resolver to perform queries.
        contentResolver = context.getContentResolver();
        accountRootList = new ArrayList<AccountRoot>();
    }

    public void load() {
        // Loading accounts from database.
        QueryHelper.getAccounts(contentResolver, accountRootList);
    }

    public void updateAccountRoot(AccountRoot accountRoot) {
        // Attempting to update account.
        if (QueryHelper.updateAccount(contentResolver, accountRoot)) {
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

    public List<AccountRoot> getAccountsList() {
        return accountRootList;
    }
}
