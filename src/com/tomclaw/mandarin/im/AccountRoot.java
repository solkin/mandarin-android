package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.os.Parcel;
import com.tomclaw.mandarin.core.CoreObject;
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
    /**
     * Service info
     */
    protected String serviceHost;
    protected int servicePort;
    /**
     * Staff
     */
    protected ContentResolver contentResolver;

    public void setContentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
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

    public abstract void connect();

    public abstract void disconnect();

    public void setStatus(int statusIndex) {
        if (this.statusIndex != statusIndex) {
            if (this.statusIndex == StatusUtil.STATUS_OFFLINE) {
                connect();
            } else if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                disconnect();
            } else {
                updateStatus(statusIndex);
            }
        }
    }

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
    }

    public void readInstanceData(Parcel in) {
        userId = in.readString();
        userNick = in.readString();
        userPassword = in.readString();
        statusIndex = in.readInt();
        statusText = in.readString();
        serviceHost = in.readString();
        servicePort = in.readInt();
    }
}
