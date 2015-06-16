package com.tomclaw.mandarin.core;

import java.io.Serializable;

/**
 * Created by solkin on 16.06.15.
 */
public class BuddyData implements Serializable {

    private int groupId;
    private String groupName;
    private String buddyId;
    private String buddyNick;
    private int statusIndex;
    private String statusTitle;
    private String statusMessage;
    private String buddyIcon;
    private long lastSeen;

    public BuddyData(int groupId, String groupName, String buddyId, String buddyNick, int statusIndex,
                     String statusTitle, String statusMessage, String buddyIcon, long lastSeen) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.buddyId = buddyId;
        this.buddyNick = buddyNick;
        this.statusIndex = statusIndex;
        this.statusTitle = statusTitle;
        this.statusMessage = statusMessage;
        this.buddyIcon = buddyIcon;
        this.lastSeen = lastSeen;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public String getBuddyNick() {
        return buddyNick;
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

    public String getBuddyIcon() {
        return buddyIcon;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
