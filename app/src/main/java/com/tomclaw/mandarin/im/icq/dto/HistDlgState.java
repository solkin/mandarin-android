package com.tomclaw.mandarin.im.icq.dto;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class HistDlgState implements Serializable {

    private String sn;

    private @Nullable Boolean starting;

    private long unreadCnt;

    private long lastMsgId;
    private long olderMsgId;
    private @Nullable Long delUpto;

    private @Nullable Yours yours;
    private @Nullable Theirs theirs;

    private @Nullable String patchVersion;

    private List<Person> persons = Collections.emptyList();
    private List<Message> messages = Collections.emptyList();

    public String getSn() {
        return sn;
    }

    public boolean isStarting() {
        if (starting != null) {
            return starting;
        }
        return false;
    }

    public @Nullable String getPatchVersion() {
        return patchVersion;
    }

    public long getDelUpTo() {
        return delUpto == null ? 0 : delUpto;
    }

    public long getLastMsgId() {
        return lastMsgId;
    }

    public long getUnreadCnt() {
        return unreadCnt;
    }

    public long getYoursLastRead() {
        return yours == null ? 0 : yours.getLastRead();
    }

    public long getTheirsLastDelivered() {
        return theirs == null ? 0 : theirs.getLastDelivered();
    }

    public long getTheirsLastRead() {
        return theirs == null ? 0 : theirs.getLastRead();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public long getOlderMsgId() {
        return olderMsgId;
    }

    public List<Person> getPersons() {
        return persons;
    }

}
