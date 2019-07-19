package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.util.Unobfuscatable;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/17/13
 * Time: 11:34 PM
 */
public class MyInfo implements Unobfuscatable {

    private String aimId;
    private String displayId;
    private String friendly;
    private String state;
    private int invisible = 0;
    private String moodIcon;
    private String moodTitle;
    private String statusMsg;
    private String userType;
    private String attachedPhoneNumber;
    private String buddyIcon;
    private String bigBuddyIcon;

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
        return invisible == 1 ? WimConstants.INVISIBLE : state;
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
        return Strings.unescapeXml(moodTitle);
    }

    public String optStatusMsg() {
        if (TextUtils.isEmpty(statusMsg)) {
            return "";
        }
        return Strings.unescapeXml(statusMsg);
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

    public String getBigBuddyIcon() {
        return bigBuddyIcon;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setMoodIcon(String moodIcon) {
        this.moodIcon = moodIcon;
    }

    public void setMoodTitle(String moodTitle) {
        this.moodTitle = moodTitle;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }
}
