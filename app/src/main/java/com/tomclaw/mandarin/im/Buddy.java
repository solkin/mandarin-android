package com.tomclaw.mandarin.im;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by ivsolkin on 12.10.16.
 */
public class Buddy implements Parcelable {

    public static final String KEY_STRUCT = "buddy_struct";

    private int accountDbId;
    @NonNull private String buddyId;

    public Buddy(int accountDbId, @NonNull String buddyId) {
        this.accountDbId = accountDbId;
        this.buddyId = buddyId;
    }

    protected Buddy(Parcel in) {
        accountDbId = in.readInt();
        buddyId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(accountDbId);
        dest.writeString(buddyId);
    }

    public int getAccountDbId() {
        return accountDbId;
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
        return buddyId.equals(buddy.buddyId);

    }

    @Override
    public int hashCode() {
        int result = accountDbId;
        result = 31 * result + buddyId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Buddy{" +
                "accountDbId=" + accountDbId +
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
