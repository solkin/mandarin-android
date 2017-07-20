package com.tomclaw.mandarin.im.icq.dto;

import java.io.Serializable;

public class Theirs implements Serializable {

    private long lastDelivered;
    private long lastRead;

    public long getLastDelivered() {
        return lastDelivered;
    }

    public long getLastRead() {
        return lastRead;
    }

}
