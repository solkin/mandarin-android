package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.Logger;

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

    public SessionHolder(Context context) {
        this.context = context;
        // Obtain content resolver to perform queries.
        contentResolver = context.getContentResolver();
        accountRootList = new ArrayList<AccountRoot>();
    }

    public void load() {
        // Loading accounts from database.
        QueryHelper.getAccounts(context, accountRootList);
    }

    public AccountRoot holdAccountRoot(int accountDbId) {
        AccountRoot accountRoot;
        try {
            // Checking for account root already in list.
            accountRoot = getAccount(accountDbId);
        } catch (AccountNotFoundException ignored) {
            // Obtain account root from database.
            accountRoot = QueryHelper.getAccount(context, accountDbId);
            accountRootList.add(accountRoot);
        }
        return accountRoot;
    }

    public boolean removeAccountRoot(int accountDbId) {
        for (AccountRoot accountRoot : accountRootList) {
            // Checking for account type and user id.
            if (accountRoot.getAccountDbId() == accountDbId) {
                // Disconnect first of all.
                // TODO: we must wait until disconnect completed!
                accountRoot.disconnect();
                // Now we ready to remove this account.
                accountRootList.remove(accountRoot);
                break;
            }
        }
        // Trying to remove all data from database, associated with this account.
        return QueryHelper.removeAccount(contentResolver, accountDbId);
    }

    public void updateAccountStatus(String accountType, String userId, int statusIndex) {
        try {
            getAccountRoot(accountType, userId).setStatus(statusIndex);
        } catch (AccountNotFoundException ignored) {
            Logger.log("Account not found while attempting to change status!");
        }
    }

    public void updateAccountStatus(String accountType, String userId, int statusIndex,
                                    String statusTitle, String statusMessage) {
        try {
            getAccountRoot(accountType, userId).setStatus(statusIndex, statusTitle, statusMessage);
        } catch (AccountNotFoundException ignored) {
            Logger.log("Account not found while attempting to change status!");
        }
    }

    public void setAutoStatus(String statusMessage) {
        for (AccountRoot accountRoot : accountRootList) {
            try {
                int musicStatusIndex = StatusUtil.getMusicStatus(accountRoot.getAccountType());
                String statusTitle = StatusUtil.getStatusTitle(accountRoot.getAccountType(), musicStatusIndex);
                accountRoot.setAutoStatus(musicStatusIndex, statusTitle, statusMessage);
            } catch (StatusNotFoundException ignored) {
                // Hm. This account doesn't support music status. Sadly :'(
            }
        }
    }

    public void resetAutoStatus() {
        for (AccountRoot accountRoot : accountRootList) {
            accountRoot.resetAutoStatus();
        }
    }

    /**
     * Connect all offline accounts with default online status.
     */
    public void connectAccounts() {
        for (AccountRoot accountRoot : accountRootList) {
            if (accountRoot.isOffline()) {
                accountRoot.setStatus(StatusUtil.getDefaultOnlineStatus(accountRoot.getAccountType()));
            }
        }
    }

    public void disconnectAccounts() {
        for (AccountRoot accountRoot : accountRootList) {
            if (accountRoot.isOnline()) {
                accountRoot.setStatus(StatusUtil.STATUS_OFFLINE);
            }
        }
    }

    private AccountRoot getAccountRoot(String accountType, String userId) throws AccountNotFoundException {
        for (AccountRoot accountRoot : accountRootList) {
            // Checking for account type and user id.
            if (accountRoot.getAccountType().equals(accountType)
                    && accountRoot.getUserId().equals(userId)) {
                return accountRoot;
            }
        }
        throw new AccountNotFoundException();
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
