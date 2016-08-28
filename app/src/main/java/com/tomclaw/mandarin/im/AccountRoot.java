package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.util.Unobfuscatable;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:54 AM
 */
public abstract class AccountRoot implements Unobfuscatable {

    /**
     * User info
     */
    protected String userId;
    protected String userNick;
    protected String userPassword;
    protected int statusIndex;
    protected String statusTitle;
    protected String statusMessage;
    protected String avatarHash;
    protected boolean connectingFlag;

    protected boolean isAutoStatus;
    protected int backupStatusIndex;
    protected String backupStatusTitle;
    protected String backupStatusMessage;
    /**
     * Staff
     */
    protected transient Context context;
    protected transient int accountDbId;

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public ContentResolver getContentResolver() {
        return context.getContentResolver();
    }

    public Resources getResources() {
        return context.getResources();
    }

    public void setAccountDbId(int accountDbId) {
        this.accountDbId = accountDbId;
    }

    public int getAccountDbId() {
        return accountDbId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public String getUserNick() {
        return userNick;
    }

    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public int getStatusIndex() {
        return statusIndex;
    }

    public String getStatusTitle() {
        return statusTitle;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public boolean isConnecting() {
        return connectingFlag;
    }

    public abstract void connect();

    public abstract void disconnect();

    /**
     * Set up logic and network status for account. Some online status will connect account
     * in case of account was offline. Offline status will disconnect account.
     *
     * @param statusIndex - non-protocol status index.
     */
    public void setStatus(int statusIndex) {
        setStatus(statusIndex, StatusUtil.getStatusTitle(getAccountType(), statusIndex), "");
    }

    /**
     * Set up logic and network status for account. Some online status will connect account
     * in case of account was offline. Offline status will disconnect account.
     *
     * @param statusIndex   - non-protocol status index.
     * @param statusTitle   - status title.
     * @param statusMessage - status description.
     */
    public void setStatus(int statusIndex, String statusTitle, String statusMessage) {
        if (getStatusIndex() != statusIndex
                || !TextUtils.equals(getStatusTitle(), statusTitle)
                || !TextUtils.equals(getStatusMessage(), statusMessage)) {
            if (getStatusIndex() == StatusUtil.STATUS_OFFLINE) {
                updateAccountState(statusIndex, statusTitle, statusMessage, true);
                connect();
            } else if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                updateAccountState(statusIndex, true);
                disconnect();
            } else {
                updateAccountState(statusIndex, statusTitle, statusMessage, false);
                // This will create request in database.
                updateStatus();
            }
        }
    }

    /**
     * This will connect account with actual status.
     */
    public void actualizeStatus() {
        // Checking for connection purpose.
        if (getStatusIndex() != StatusUtil.STATUS_OFFLINE) {
            // Update account state in database.
            updateAccountState(true);
            // Yeah, connect!
            connect();
        } else if (isConnecting()) {
            // Disconnection process is not completed. Let's became offline.
            updateAccountState(StatusUtil.STATUS_OFFLINE, false);
        }
    }

    public void setAutoStatus(int statusIndex, String statusTitle, String statusMessage) {
        // Checking for we are here right now.
        if (!isOffline()) {
            // Backup manual user status.
            backupStatus();
            // Update current status.
            setStatus(statusIndex, statusTitle, statusMessage);
        }
    }

    public void resetAutoStatus() {
        // Trying to restore status.
        if (restoreStatus()) {
            // Status was restored.
            updateStatus();
        }
    }

    private void backupStatus() {
        // Checking for this is not already auto-status.
        // In case of auto-status we ready to replace it, but save original.
        if (!isAutoStatus) {
            backupStatusIndex = statusIndex;
            backupStatusTitle = statusTitle;
            backupStatusMessage = statusMessage;
            isAutoStatus = true;
        }
    }

    private boolean restoreStatus() {
        if (isAutoStatus) {
            statusIndex = backupStatusIndex;
            statusTitle = backupStatusTitle;
            statusMessage = backupStatusMessage;
            isAutoStatus = false;
            return true;
        }
        return false;
    }

    public abstract void checkCredentials(CredentialsCheckCallback callback);

    /**
     * This will manual disconnect account after network connection stopped.
     * Invokes after account connection closed.
     */
    public void carriedOff() {
        updateAccountState(StatusUtil.STATUS_OFFLINE, false);
    }

    public boolean isOffline() {
        return getStatusIndex() == StatusUtil.STATUS_OFFLINE && !isConnecting();
    }

    public boolean isOnline() {
        return getStatusIndex() != StatusUtil.STATUS_OFFLINE && !isConnecting();
    }

    /**
     * Setup only connecting flag and updates account in database.
     *
     * @param isConnecting - connecting flag.
     */
    protected void updateAccountState(boolean isConnecting) {
        this.connectingFlag = isConnecting;
        updateAccount();
    }

    /**
     * Setup status index with default status title and empty message,
     * setup connecting flag and update account in database.
     *
     * @param statusIndex  - non-protocol status index.
     * @param isConnecting - connecting flag.
     */
    protected void updateAccountState(int statusIndex, boolean isConnecting) {
        updateAccountState(statusIndex, StatusUtil.getStatusTitle(getAccountType(), statusIndex), "", isConnecting);
    }

    /**
     * Setup status index, title, message and connecting flag and updates account in database.
     *
     * @param statusIndex   - non-protocol status index.
     * @param statusTitle   - status title
     * @param statusMessage - status description
     * @param isConnecting  - connecting flag.
     */
    protected void updateAccountState(int statusIndex, String statusTitle, String statusMessage,
                                      boolean isConnecting) {
        // Setup local variables.
        this.statusIndex = statusIndex;
        this.statusTitle = statusTitle;
        this.statusMessage = statusMessage;
        this.connectingFlag = isConnecting;
        // Save account data in database.
        updateAccount();
    }

    /**
     * This will update account info in database
     */
    public void updateAccount() {
        // Update database info.
        QueryHelper.updateAccount(context, this);
    }

    /**
     * Update protocol online status.
     */
    public abstract void updateStatus();

    public abstract String getAccountType();
}
