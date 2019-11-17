package com.tomclaw.mandarin.im.icq.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Message implements Serializable {

    @SerializedName("msgId")
    private long msgId;
    @SerializedName("reqId")
    private String reqId;
    @SerializedName("outgoing")
    private boolean outgoing;
    @SerializedName("wid")
    private String wid;
    @SerializedName("time")
    private long time;
    @SerializedName("text")
    private String text;
    @SerializedName("mediaType")
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
