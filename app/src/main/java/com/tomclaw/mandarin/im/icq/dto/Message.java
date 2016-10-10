package com.tomclaw.mandarin.im.icq.dto;

public class Message {

    private long msgId;
    private String reqId;
    private boolean outgoing;
    private String wid;
    private long time;
    private String text;
    private String mediaType;

    public long getMsgId() {
        return msgId;
    }

    public String getReqId() {
        return reqId;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public String getWid() {
        return wid;
    }

    public long getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public String getMediaType() {
        return mediaType;
    }

}
