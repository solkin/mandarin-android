package com.tomclaw.mandarin.im.icq.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by ivsolkin on 26.11.16.
 */

public class HistoryMessages implements Serializable {

    private List<Message> messages = Collections.emptyList();

    public List<Message> getMessages() {
        return messages;
    }
}
