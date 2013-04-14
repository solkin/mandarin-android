package com.tomclaw.mandarin.im;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:54 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AccountRoot {

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
    protected int serviceId;
    protected String serviceHost;
    protected int servicePort;
    /**
     * User data
     */
    protected List<GroupItem> buddyItems = new ArrayList<GroupItem>();

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

    public abstract int getServiceIcon();

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(userNick);
        dest.writeString(userPassword);
        dest.writeInt(statusIndex);
        dest.writeString(statusText);
        dest.writeInt(serviceId);
        dest.writeString(serviceHost);
        dest.writeInt(servicePort);
        dest.writeTypedList(buddyItems);
    }

    public void readFromParcel(Parcel in) {
        userId = in.readString();
        userNick = in.readString();
        userPassword = in.readString();
        statusIndex = in.readInt();
        statusText = in.readString();
        serviceId = in.readInt();
        serviceHost = in.readString();
        servicePort = in.readInt();
        in.readList(buddyItems, GroupItem.class.getClassLoader());
    }
}
