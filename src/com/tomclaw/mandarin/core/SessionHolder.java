package com.tomclaw.mandarin.core;

import android.util.Log;
import com.tomclaw.mandarin.im.BuddyItem;
import com.tomclaw.mandarin.im.GroupItem;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;

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

    private List<IcqAccountRoot> accountRootList = new ArrayList<IcqAccountRoot>();

    public void load() {
        // Loading accounts from local storage.
        for (int c = 0; c < 5; c++) {
            IcqAccountRoot accountRoot = new IcqAccountRoot();
            accountRoot.setUserId("7068514");
            accountRoot.setUserNick("Solkin");
            accountRoot.setUserPassword("112");
            accountRoot.setSomeStaff("staff" + c);
            for(int i=0;i<10;i++){
                GroupItem groupItem = new GroupItem("Group " + i);
                List<BuddyItem> buddyItems = groupItem.getItems();
                for(int j=0;j<20;j++){
                    buddyItems.add(new BuddyItem("User " + j, "user"+j+"@molecus.com"));
                }
                accountRoot.getGroupItems().add(groupItem);
            }
            accountRootList.add(accountRoot);
        }
        Log.d(Settings.LOG_TAG, "loaded " + accountRootList.size() + " accounts");
    }

    public void save() {
        // Saving account to local storage.
    }

    public List<IcqAccountRoot> getAccountsList() {
        return accountRootList;
    }
}
