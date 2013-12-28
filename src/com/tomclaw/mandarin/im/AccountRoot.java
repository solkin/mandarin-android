package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import com.tomclaw.mandarin.core.QueryHelper;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:54 AM
 */
public abstract class AccountRoot {

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
    /**
     * Service info
     */
    protected String serviceHost;
    protected int servicePort;
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
        if (this.statusIndex != statusIndex) {
            if (this.statusIndex == StatusUtil.STATUS_OFFLINE) {
                updateAccountState(statusIndex, true);
                connect();
            } else if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                updateAccountState(true);
                disconnect();
            } else {
                updateAccountState(statusIndex, false);
                // This will create request in database.
                updateStatus(statusIndex);
            }
        }
    }

    /**
     * This will connect account with actual status.
     */
    public void actualizeStatus() {
        // Checking for connection purpose.
        if (statusIndex != StatusUtil.STATUS_OFFLINE) {
            // Update account state in database.
            updateAccountState(true);
            // Yeah, connect!
            connect();
        }
    }

    /**
     * This will manual disconnect account after network connection stopped.
     * Invokes after account connection closed.
     */
    public void carriedOff() {
        updateAccountState(StatusUtil.STATUS_OFFLINE, false);
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
     * @param statusIndex - non-protocol status index.
     * @param isConnecting - connecting flag.
     */
    protected void updateAccountState(int statusIndex, boolean isConnecting) {
        updateAccountState(statusIndex, StatusUtil.getStatusTitle(getAccountType(), statusIndex), "", isConnecting);
    }

    /**
     * Setup status index, title, message and connecting flag and updates account in database.
     * @param statusIndex - non-protocol status index.
     * @param statusTitle - status title
     * @param statusMessage - status description
     * @param isConnecting - connecting flag.
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
     *
     * @param statusIndex - non-protocol status index.
     */
    public abstract void updateStatus(int statusIndex);

    public abstract String getAccountType();

    public abstract int getAccountLayout();
}
