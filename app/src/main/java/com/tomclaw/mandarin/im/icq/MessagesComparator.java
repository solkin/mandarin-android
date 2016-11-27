package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.im.icq.dto.Message;

import java.util.Comparator;

/**
 * Created by ivsolkin on 27.11.16.
 */
public class MessagesComparator implements Comparator<Message> {

    @Override
    public int compare(Message o1, Message o2) {
        return compare(o1.getMsgId(), o2.getMsgId());
    }

    private int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
