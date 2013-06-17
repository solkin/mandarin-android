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

    private transient IcqSession icqSession;

    public IcqAccountRoot() {
        icqSession = new IcqSession(this);
    }

    @Override
    public void connect() {
        Thread connectThread = new Thread() {
            public void run() {
                icqSession.clientLogin();
                updateAccountState(false);
            }
        };
        connectThread.start();
    }

    @Override
    public void disconnect() {
    }

    public void updateStatus(int statusIndex) {

    }

    @Override
    public String getAccountType() {
        return getClass().getName();
    }

    public static int[] getStatusResources() {
        return new int[]{
                R.drawable.status_icq_offline,
                R.drawable.status_icq_mobile,
                R.drawable.status_icq_online,
                R.drawable.status_icq_invisible,
                R.drawable.status_icq_chat,
                R.drawable.status_icq_away,
                R.drawable.status_icq_dnd,
                R.drawable.status_icq_na,
                R.drawable.status_icq_busy
        };
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
