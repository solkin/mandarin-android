package com.tomclaw.mandarin.main;

/**
 * Created by Solkin on 30.11.2014.
 */
public class ChatHistoryItem {

    private long messageId;
    private long messagePrevId;
    private long messageDbId;
    private int messageType;
    private CharSequence messageText;
    private long messageTime;
    private String messageCookie;
    private int contentType;
    private long contentSize;
    private int contentState;
    private int contentProgress;
    private String contentName;
    private String contentUri;
    private String previewHash;
    private String contentTag;
    private String messageTimeText;
    private String messageDateText;
    private boolean dateVisible;

    public ChatHistoryItem(long messageId, long messagePrevId, long messageDbId,
                           int messageType, CharSequence messageText,
                           long messageTime, String messageCookie, int contentType,
                           long contentSize, int contentState, int contentProgress,
                           String contentName, String contentUri, String previewHash,
                           String contentTag, String messageTimeText, String messageDateText,
                           boolean dateVisible) {
        this.messageId = messageId;
        this.messagePrevId = messagePrevId;
        this.messageDbId = messageDbId;
        this.messageType = messageType;
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageCookie = messageCookie;
        this.contentType = contentType;
        this.contentSize = contentSize;
        this.contentState = contentState;
        this.contentProgress = contentProgress;
        this.contentName = contentName;
        this.contentUri = contentUri;
        this.previewHash = previewHash;
        this.contentTag = contentTag;
        this.messageTimeText = messageTimeText;
        this.messageDateText = messageDateText;
        this.dateVisible = dateVisible;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getMessagePrevId() {
        return messagePrevId;
    }

    public long getMessageDbId() {
        return messageDbId;
    }

    public int getMessageType() {
        return messageType;
    }

    public CharSequence getMessageText() {
        return messageText;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public String getMessageCookie() {
        return messageCookie;
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

    public String getContentName() {
        return contentName;
    }

    public String getContentUri() {
        return contentUri;
    }

    public String getPreviewHash() {
        return previewHash;
    }

    public String getContentTag() {
        return contentTag;
    }

    public String getMessageTimeText() {
        return messageTimeText;
    }

    public String getMessageDateText() {
        return messageDateText;
    }

    public boolean isDateVisible() {
        return dateVisible;
    }
}
