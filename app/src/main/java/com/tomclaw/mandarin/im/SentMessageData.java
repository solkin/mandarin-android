package com.tomclaw.mandarin.im;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by solkin on 05.07.17.
 */
public class SentMessageData implements Parcelable {

    private String cookie;
    private long msgId;
    private long prevMsgId;
    private long time;

    public SentMessageData(String cookie, long msgId, long prevMsgId, long time) {
        this.cookie = cookie;
        this.msgId = msgId;
        this.prevMsgId = prevMsgId;
        this.time = time;
    }

    protected SentMessageData(Parcel in) {
        cookie = in.readString();
        msgId = in.readLong();
        prevMsgId = in.readLong();
        time = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cookie);
        dest.writeLong(msgId);
        dest.writeLong(prevMsgId);
        dest.writeLong(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SentMessageData> CREATOR = new Creator<SentMessageData>() {
        @Override
        public SentMessageData createFromParcel(Parcel in) {
            return new SentMessageData(in);
        }

        @Override
        public SentMessageData[] newArray(int size) {
            return new SentMessageData[size];
        }
    };

    public String getCookie() {
        return cookie;
    }

    public long getMessageId() {
        return msgId;
    }

    public long getMessagePrevId() {
        return prevMsgId;
    }

    public long getMessageTime() {
        return time;
    }
}
