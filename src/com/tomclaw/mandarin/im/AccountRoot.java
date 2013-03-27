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
    private String serviceHost;
    private int servicePort;
    /** User data **/
    private List<GroupItem> buddyItems = new ArrayList<GroupItem>();
}
