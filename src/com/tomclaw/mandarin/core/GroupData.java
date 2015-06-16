package com.tomclaw.mandarin.core;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by solkin on 16.06.15.
 */
public class GroupData implements Serializable {

    private String groupName;
    private int groupId;
    private ArrayList<BuddyData> buddyDatas;

    public GroupData(String groupName, int groupId, ArrayList<BuddyData> buddyDatas) {
        this.groupName = groupName;
        this.groupId = groupId;
        this.buddyDatas = buddyDatas;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getGroupId() {
        return groupId;
    }

    public ArrayList<BuddyData> getBuddyDatas() {
        return buddyDatas;
    }
}
