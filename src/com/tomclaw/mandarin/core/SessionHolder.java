package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.im.AccountRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 2:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class SessionHolder {

    private List<AccountRoot> accountRootList = new ArrayList<AccountRoot>();

    public void load() {
        // Loading accounts from local storage.
    }

    public void save() {
        // Saving account to local storage.
    }
}
