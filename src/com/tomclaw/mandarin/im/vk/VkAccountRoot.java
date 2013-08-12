package com.tomclaw.mandarin.im.vk;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 8/7/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class VkAccountRoot extends AccountRoot{

    private String token;
    private long tokenExpirationDate;

    private transient VKSession vkSession;

    public VkAccountRoot() {
        vkSession = new VKSession(this);
    }

    public void writeInstanceData(Parcel dest) {
        super.writeInstanceData(dest);
        dest.writeString(token);
        dest.writeLong(tokenExpirationDate);
    }

    public void readInstanceData(Parcel in) {
        super.readInstanceData(in);
        token = in.readString();
        tokenExpirationDate = in.readLong();
    }

    public VKSession getSession() {
        return vkSession;
    }

    public void setToken(String token){
        this.token = token;
    }

    public String getToken(){
        return token;
    }

    public void setTokenExpirationDate(long time){
        this.tokenExpirationDate = time;
    }

    public long getTokenExpirationDate(){
        return tokenExpirationDate;
    }

    @Override
    public void connect() {
        Log.d(Settings.LOG_TAG, "vk connection attempt");
        Thread connectThread = new Thread() {
            private void sleep() {
                try {
                    sleep(5000);
                } catch (InterruptedException ignored) {
                    // No need to check.
                }
            }

            public void run() {
                do {
                    while (!checkLoginReady()) {
                        /*Получить токен*/
                    }
                    switch (vkSession.clientLogin()) {
                        case VKSession.LOGIN_ERROR: {
                            // Show notification.
                            updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                            return;
                        }
                    }
                    // Update account connecting state to false.
                    updateAccountState(false);
                    // Starting events fetching in verbal cycle.
                } while (!vkSession.startEventsFetching());
                // Update offline status.
                updateAccountState(StatusUtil.STATUS_OFFLINE, false);
            }
        };
        connectThread.start();
    }

    private boolean checkLoginReady() {
        return (!TextUtils.isEmpty(token) /*&& System.currentTimeMillis() / 1000L < tokenExpirationDate*/);
    }

    @Override
    public void disconnect() {
        vkSession.disconnect();
        updateAccountState(StatusUtil.STATUS_OFFLINE, false);
    }

    @Override
    public void updateStatus(int statusIndex) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAccountType() {
        return getClass().getName();
    }

    @Override
    public int getAccountLayout() {
        return R.layout.account_add_web;
    }
}
