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
public class GroupItem implements Parcelable {

    /** Group info **/
    private String groupName;
    /** Group data **/
    private List<BuddyItem> items = new ArrayList<BuddyItem>();

    public GroupItem(String groupName) {
        this.groupName = groupName;
    }

    public List<BuddyItem> getItems() {
        return items;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(groupName);
        dest.writeTypedList(items);
    }

    public  void readFromParcel(Parcel in) {
        groupName = in.readString();
        items = in.createTypedArrayList(BuddyItem.CREATOR);
    }

    private GroupItem(Parcel in) {
        readFromParcel(in);
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
