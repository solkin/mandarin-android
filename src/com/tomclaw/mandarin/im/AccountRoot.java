package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import com.tomclaw.mandarin.core.CoreObject;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:54 AM
 */
public abstract class AccountRoot extends CoreObject {

    /**
     * User info
     */
    protected String userId;
    protected String userNick;
    protected String userPassword;
    protected int statusIndex;
    protected String statusText;
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

    public Context getContext(){
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
                updateAccountState(statusIndex, false);
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
            updateAccountState(statusIndex, true);
            // Yeah, connect!
            connect();
        }
    }

    /**
     * Setup only connecting flag and updates account in database.
     *
     * @param isConnecting - connecting flag.
     */
    protected void updateAccountState(boolean isConnecting) {
        updateAccountState(statusIndex, isConnecting);
    }

    /**
     * Setup status index and connecting flag and updates account in database.
     *
     * @param statusIndex - non-protocol status index.
     * @param isConnecting - connecting flag.
     */
    protected void updateAccountState(int statusIndex, boolean isConnecting) {
        // Setup local variables.
        this.statusIndex = statusIndex;
        connectingFlag = isConnecting;
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
     * Update online status.
     *
     * @param statusIndex - non-protocol status index.
     */
    public abstract void updateStatus(int statusIndex);

    public abstract String getAccountType();

    public abstract int getAccountLayout();

    public void writeInstanceData(Parcel dest) {
        dest.writeString(userId);
        dest.writeString(userNick);
        dest.writeString(userPassword);
        dest.writeInt(statusIndex);
        dest.writeString(statusText);
        dest.writeString(serviceHost);
        dest.writeInt(servicePort);
        dest.writeInt(connectingFlag ? 1 : 0);
    }

    public void readInstanceData(Parcel in) {
        userId = in.readString();
        userNick = in.readString();
        userPassword = in.readString();
        statusIndex = in.readInt();
        statusText = in.readString();
        serviceHost = in.readString();
        servicePort = in.readInt();
        connectingFlag = in.readInt() == 1;
    }
}
