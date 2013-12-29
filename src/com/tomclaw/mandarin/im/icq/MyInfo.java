package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/17/13
 * Time: 11:34 PM
 */
public class MyInfo {

    private String aimId;
    private String displayId;
    private String friendly;
    private String state;
    private String moodIcon;
    private String moodTitle;
    private String statusMsg;
    private String userType;
    private String attachedPhoneNumber;
    private String buddyIcon;

    public String getAimId() {
        return aimId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getFriendly() {
        return friendly;
    }

    public String getState() {
        return state;
    }

    public String optMoodIcon() {
        if (TextUtils.isEmpty(moodIcon)) {
            return "";
        }
        return moodIcon;
    }

    public String optMoodTitle() {
        if (TextUtils.isEmpty(moodTitle)) {
            return "";
        }
        return moodTitle;
    }

    public String optStatusMsg() {
        if (TextUtils.isEmpty(statusMsg)) {
            return "";
        }
        return statusMsg;
    }

    public String getUserType() {
        return userType;
    }

    public String getAttachedPhoneNumber() {
        return attachedPhoneNumber;
    }

    public String getBuddyIcon() {
        return buddyIcon;
    }
}
