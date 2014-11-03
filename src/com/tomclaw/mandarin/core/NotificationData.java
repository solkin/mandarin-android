package com.tomclaw.mandarin.core;

import java.io.Serializable;

/**
 * Created by Solkin on 31.08.2014.
 */
public class NotificationData implements Serializable {

    private String messageText;
    private int buddyDbId;
    private String buddyNick;
    private String buddyAvatarHash;
    private int unreadCount;
    private int contentType;
    private String previewHash;

    public NotificationData(String messageText, int buddyDbId, String buddyNick, String buddyAvatarHash,
                            int unreadCount, int contentType, String previewHash) {
        this.messageText = messageText;
        this.buddyDbId = buddyDbId;
        this.buddyNick = buddyNick;
        this.buddyAvatarHash = buddyAvatarHash;
        this.unreadCount = unreadCount;
        this.contentType = contentType;
        this.previewHash = previewHash;
    }

    public String getMessageText() {
        return messageText;
    }

    public int getBuddyDbId() {
        return buddyDbId;
    }

    public String getBuddyNick() {
        return buddyNick;
    }

    public String getBuddyAvatarHash() {
        return buddyAvatarHash;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public int getContentType() {
        return contentType;
    }

    public String getPreviewHash() {
        return previewHash;
    }
}
