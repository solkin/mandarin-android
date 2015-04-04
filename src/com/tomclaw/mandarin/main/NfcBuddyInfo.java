package com.tomclaw.mandarin.main;

import com.tomclaw.mandarin.util.Unobfuscatable;

/**
 * Created by solkin on 03.04.15.
 */
public class NfcBuddyInfo implements Unobfuscatable {

    private String accountType;
    private String buddyId;
    private String buddyNick;
    private int buddyStatus;

    public NfcBuddyInfo() {
    }

    public NfcBuddyInfo(String accountType, String buddyId, String buddyNick, int buddyStatus) {
        this.accountType = accountType;
        this.buddyId = buddyId;
        this.buddyNick = buddyNick;
        this.buddyStatus = buddyStatus;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public String getBuddyNick() {
        return buddyNick;
    }

    public int getBuddyStatus() {
        return buddyStatus;
    }
}
