package com.tomclaw.mandarin.im;

import android.os.Parcel;
import androidx.annotation.NonNull;

/**
 * Created by ivsolkin on 12.10.16.
 */
public class StrictBuddy extends Buddy {

    public static final String KEY_STRUCT = "strict_buddy_struct";

    @NonNull
    private String groupName;

    public StrictBuddy(int accountDbId, @NonNull String groupName, @NonNull String buddyId) {
        super(accountDbId, buddyId);
        this.groupName = groupName;
    }

    protected StrictBuddy(Parcel in) {
        super(in);
        groupName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(groupName);
    }

    public
    @NonNull
    String getGroupName() {
        return groupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StrictBuddy that = (StrictBuddy) o;

        return groupName.equals(that.groupName);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + groupName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StrictBuddy{" +
                "groupName='" + groupName + '\'' +
                '}';
    }

    public static final Creator<StrictBuddy> CREATOR = new Creator<StrictBuddy>() {
        @Override
        public StrictBuddy createFromParcel(Parcel in) {
            return new StrictBuddy(in);
        }

        @Override
        public StrictBuddy[] newArray(int size) {
            return new StrictBuddy[size];
        }
    };
}
