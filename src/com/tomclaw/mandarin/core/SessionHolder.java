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
        QueryHelper.updateAccount(contentResolver, accountRoot);
    }

    public void removeAccountRoot(AccountRoot accountRoot) {

    }

    public List<AccountRoot> getAccountsList() {
        return accountRootList;
    }
}
