package com.tomclaw.mandarin.im;

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

    /** User info **/
    private String userId;
    private String userNick;
    private String userPassword;
    private int statusIndex;
    private String statusText;
    /** Service info **/
    private int serviceId;
    private String serviceHost;
    private int servicePort;
    /** User data **/
    private List<GroupItem> buddyItems = new ArrayList<GroupItem>();

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
}
