package com.tomclaw.mandarin.im.icq.dto;

import java.io.Serializable;

/**
 * Created by ivsolkin on 27.11.16.
 */
public class Patch implements Serializable {

    private PatchType type;
    private String msgId;

    public PatchType getType() {
        return type;
    }

    public String getMsgId() {
        return msgId;
    }
}
