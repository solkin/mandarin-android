package com.tomclaw.mandarin.im.icq;

import android.os.Parcel;
import android.os.Parcelable;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 3/31/13
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class IcqAccountRoot extends AccountRoot implements Parcelable {
    private String someStuff;

    public void setSomeStaff(String stuff){
        someStuff = stuff;
    }

    public String getSomeStuff(){
        return someStuff;
    }

    public IcqAccountRoot(){
    }

    @Override
    public int getServiceIcon() {
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(someStuff);
        super.writeToParcel(dest, flags);
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
        someStuff = in.readString();
        readFromParcel(in);
    }
}
