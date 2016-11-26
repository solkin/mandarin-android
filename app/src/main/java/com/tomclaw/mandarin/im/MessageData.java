package com.tomclaw.mandarin.im;

/**
 * Created by ivsolkin on 26.11.16.
 */

public class MessageData {

    private int buddyAccountDbId;
    private String buddyId;
    private long messagePrevId;
    private long messageId;
    private String cookie;
    private int messageType;
    private long messageTime;
    private String messageText;
    private int contentType;
    private long contentSize;
    private int contentState;
    private int contentProgress;
    private String contentUri;
    private String contentName;
    private String previewHash;
    private String contentTag;

    public MessageData(int buddyAccountDbId, String buddyId, long messagePrevId, long messageId,
                       String cookie, int messageType, long messageTime, String messageText) {
        this.buddyAccountDbId = buddyAccountDbId;
        this.buddyId = buddyId;
        this.messagePrevId = messagePrevId;
        this.messageId = messageId;
        this.cookie = cookie;
        this.messageType = messageType;
        this.messageTime = messageTime;
        this.messageText = messageText;
    }

    public MessageData(int buddyAccountDbId, String buddyId, long messagePrevId, long messageId,
                       String cookie, int messageType, long messageTime, String messageText,
                       int contentType, long contentSize, int contentState, int contentProgress,
                       String contentUri, String contentName, String previewHash, String contentTag) {
        this.buddyAccountDbId = buddyAccountDbId;
        this.buddyId = buddyId;
        this.messagePrevId = messagePrevId;
        this.messageId = messageId;
        this.cookie = cookie;
        this.messageType = messageType;
        this.messageTime = messageTime;
        this.messageText = messageText;
        this.contentType = contentType;
        this.contentSize = contentSize;
        this.contentState = contentState;
        this.contentProgress = contentProgress;
        this.contentUri = contentUri;
        this.contentName = contentName;
        this.previewHash = previewHash;
        this.contentTag = contentTag;
    }

    public int getBuddyAccountDbId() {
        return buddyAccountDbId;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public long getMessagePrevId() {
        return messagePrevId;
    }

    public long getMessageId() {
        return messageId;
    }

    public String getCookie() {
        return cookie;
    }

    public int getMessageType() {
        return messageType;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public String getMessageText() {
        return messageText;
    }

    public int getContentType() {
        return contentType;
    }

    public long getContentSize() {
        return contentSize;
    }

    public int getContentState() {
        return contentState;
    }

    public int getContentProgress() {
        return contentProgress;
    }

    public String getContentUri() {
        return contentUri;
    }

    public String getContentName() {
        return contentName;
    }

    public String getPreviewHash() {
        return previewHash;
    }

    public String getContentTag() {
        return contentTag;
    }
}
