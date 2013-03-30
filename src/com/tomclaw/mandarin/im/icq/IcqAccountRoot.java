package com.tomclaw.mandarin.im.icq;

import android.os.Parcel;
import android.os.Parcelable;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.GroupItem;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 3/31/13
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class IcqAccountRoot extends AccountRoot implements Parcelable {

    public IcqAccountRoot(){
    }

    @Override
    public int getServiceIcon() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int describeContents() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(userNick);
        dest.writeString(userPassword);
        dest.writeInt(statusIndex);
        dest.writeString(statusText);
        dest.writeInt(serviceId);
        dest.writeString(serviceHost);
        dest.writeInt(servicePort);
        dest.writeTypedList(buddyItems);
    }

    public static final Parcelable.Creator<IcqAccountRoot> CREATOR = new Parcelable.Creator<IcqAccountRoot>() {

        @Override
        public IcqAccountRoot createFromParcel(Parcel source) {
            return new IcqAccountRoot(source);
        }

        @Override
        public IcqAccountRoot[] newArray(int size) {
            return new IcqAccountRoot[size];
        }
    };

    private IcqAccountRoot(Parcel in){
        userId = in.readString();
        userNick = in.readString();
        userPassword = in.readString();
        statusIndex = in.readInt();
        statusText = in.readString();
        serviceId = in.readInt();
        serviceHost = in.readString();
        servicePort = in.readInt();
        in.readList(buddyItems, GroupItem.class.getClassLoader());
    }
}
