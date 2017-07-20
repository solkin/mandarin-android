package com.tomclaw.mandarin.im.icq.dto;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by ivsolkin on 26.11.16.
 */
public class HistoryMessages implements Serializable {

    private long unreadCnt;

    private long lastMsgId;
    private long olderMsgId;
    private long newerMsgId;

    private @Nullable Yours yours;
    private @Nullable Theirs theirs;

    private @Nullable String patchVersion;

    private List<Person> persons = Collections.emptyList();
    private List<Message> messages = Collections.emptyList();
    private List<Patch> patch = Collections.emptyList();

    public List<Message> getMessages() {
        return messages;
    }
}
