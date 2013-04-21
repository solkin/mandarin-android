package com.tomclaw.mandarin.im;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class BuddyItem implements Parcelable {

    /**
     * Buddy info
     */
    private String buddyId;
    private String buddyNick;

    public BuddyItem(String buddyId, String buddyNick) {
        this.buddyId = buddyId;
        this.buddyNick = buddyNick;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public void setBuddyId(String buddyId) {
        this.buddyId = buddyId;
    }

    public String getBuddyNick() {
        return buddyNick;
    }

    public void setBuddyNick(String buddyNick) {
        this.buddyNick = buddyNick;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(buddyId);
        dest.writeString(buddyNick);
    }

    private BuddyItem(Parcel in) {
        buddyId = in.readString();
        buddyNick = in.readString();
    }

    public static final Parcelable.Creator<BuddyItem> CREATOR = new Parcelable.Creator<BuddyItem>() {

        @Override
        public BuddyItem createFromParcel(Parcel source) {
            return new BuddyItem(source);
        }

        @Override
        public BuddyItem[] newArray(int size) {
            return new BuddyItem[size];
        }
    };
}
