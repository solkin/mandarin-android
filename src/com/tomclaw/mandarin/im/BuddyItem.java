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
public class BuddyItem implements Parcelable{

    /** Buddy info **/
    private String buddyId;
    private String buddyNick;

    @Override
    public int describeContents() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(buddyId);
        dest.writeString(buddyNick);
    }

    private BuddyItem(Parcel in){
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
