package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import com.tomclaw.mandarin.util.Unobfuscatable;

import static com.tomclaw.mandarin.im.icq.WimConstants.FALLBACK_STATE;

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
    private String state = FALLBACK_STATE;
    private int invisible = 0;
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
        return invisible == 1 ? WimConstants.INVISIBLE : state;
    }

    public String optMoodTitle() {
        if (TextUtils.isEmpty(moodTitle)) {
            return "";
        }
        return StringUtil.unescapeXml(moodTitle);
    }

    public String optStatusMsg() {
        if (TextUtils.isEmpty(statusMsg)) {
            return "";
        }
        return StringUtil.unescapeXml(statusMsg);
    }

    public String getUserType() {
        return userType;
    }

    public String getAttachedPhoneNumber() {
        return attachedPhoneNumber;
    }

    public String getBuddyIcon() {
        return HttpUtil.getAvatarUrl(aimId);
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setMoodTitle(String moodTitle) {
        this.moodTitle = moodTitle;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }
}
