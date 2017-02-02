package com.tomclaw.mandarin.im.icq.dto;

import java.io.Serializable;

public class Person implements Serializable {

    private String sn;
    private String friendly;

    public String getSn() {
        return sn;
    }

    public String getFriendly() {
        return friendly;
    }

}
