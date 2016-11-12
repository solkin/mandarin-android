package com.tomclaw.mandarin.im;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by ivsolkin on 12.10.16.
 */
public class Buddy implements Parcelable {

    public static final String KEY_BUDDY_STRUCT = "buddy_struct";

    private int accountDbId;
    @Nullable private String groupName;
    @NonNull private String buddyId;

    public Buddy(int accountDbId, String buddyId) {
        this(accountDbId, null, buddyId);
    }

    public Buddy(int accountDbId, @Nullable String groupName, @NonNull String buddyId) {
        this.accountDbId = accountDbId;
        this.groupName = groupName;
        this.buddyId = buddyId;
    }

    protected Buddy(Parcel in) {
        accountDbId = in.readInt();
        groupName = in.readString();
        buddyId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(accountDbId);
        dest.writeString(groupName);
        dest.writeString(buddyId);
    }

    public int getAccountDbId() {
        return accountDbId;
    }

    public @Nullable String getGroupName() {
        return groupName;
    }

    public boolean isGroupClarified() {
        return !TextUtils.isEmpty(groupName);
    }

    public @NonNull String getBuddyId() {
        return buddyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Buddy buddy = (Buddy) o;

        if (accountDbId != buddy.accountDbId) return false;
        if (groupName != null ? !groupName.equals(buddy.groupName) : buddy.groupName != null)
            return false;
        return buddyId.equals(buddy.buddyId);

    }

    @Override
    public int hashCode() {
        int result = accountDbId;
        result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
        result = 31 * result + buddyId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Buddy{" +
                "accountDbId=" + accountDbId +
                ", groupName='" + groupName + '\'' +
                ", buddyId='" + buddyId + '\'' +
                '}';
    }

    public static final Creator<Buddy> CREATOR = new Creator<Buddy>() {
        @Override
        public Buddy createFromParcel(Parcel in) {
            return new Buddy(in);
        }

        @Override
        public Buddy[] newArray(int size) {
            return new Buddy[size];
        }
    };
}
