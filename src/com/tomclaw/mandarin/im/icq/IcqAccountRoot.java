package com.tomclaw.mandarin.im.icq;

import android.os.Parcel;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 3/31/13
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class IcqAccountRoot extends AccountRoot {
    private String someStuff;

    public IcqAccountRoot() {
    }

    public void setSomeStaff(String stuff) {
        someStuff = stuff;
    }

    public String getSomeStuff() {
        return someStuff;
    }

    @Override
    public int getServiceIcon() {
        return 0;
    }

    @Override
    public int getAccountLayout() {
        return R.layout.account_add;
    }

    public void writeInstanceData(Parcel dest) {
        super.writeInstanceData(dest);
        dest.writeString(someStuff);
    }

    public void readInstanceData(Parcel in) {
        super.readInstanceData(in);
        someStuff = in.readString();
    }
}
