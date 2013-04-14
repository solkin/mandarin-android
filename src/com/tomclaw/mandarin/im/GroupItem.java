package com.tomclaw.mandarin.im;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class GroupItem implements Parcelable{

    /** Group info **/
    private String groupName;
    /** Group data **/
    private List<BuddyItem> items = new ArrayList<BuddyItem>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(groupName);
        dest.writeTypedList(items);
    }

    private GroupItem(Parcel in){
        groupName = in.readString();
        in.readList(items, BuddyItem.class.getClassLoader());
    }

    public static final Parcelable.Creator<GroupItem> CREATOR = new Parcelable.Creator<GroupItem>() {

        @Override
        public GroupItem createFromParcel(Parcel source) {
            return new GroupItem(source);
        }

        @Override
        public GroupItem[] newArray(int size) {
            return new GroupItem[size];
        }
    };
}
