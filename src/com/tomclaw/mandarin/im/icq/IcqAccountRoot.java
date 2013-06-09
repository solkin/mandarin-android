package com.tomclaw.mandarin.im.icq;

import android.os.Parcel;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 3/31/13
 * Time: 12:47 AM
 */
public class IcqAccountRoot extends AccountRoot {

    public IcqAccountRoot() {
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public int getAccountType() {
        return ACCOUNT_TYPE_ICQ;
    }

    @Override
    public int getAccountLayout() {
        return R.layout.account_add_icq;
    }

    public void writeInstanceData(Parcel dest) {
        super.writeInstanceData(dest);
    }

    public void readInstanceData(Parcel in) {
        super.readInstanceData(in);
    }
}
