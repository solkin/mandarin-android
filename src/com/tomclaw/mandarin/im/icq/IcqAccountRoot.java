package com.tomclaw.mandarin.im.icq;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 3/31/13
 * Time: 12:47 AM
 */
public class IcqAccountRoot extends AccountRoot {

    // Network session.
    private transient IcqSession icqSession;
    // Client login variables.
    private String tokenA;
    private String sessionKey;
    private long tokenExpirationDate;
    private long timeDelta;
    // Start session variables.
    private String aimSid;
    private String fetchBaseURL;
    private MyInfo myInfo;
    private WellKnownUrls wellKnownUrls;

    public IcqAccountRoot() {
        icqSession = new IcqSession(this);
    }

    @Override
    public void connect() {
        Log.d(Settings.LOG_TAG, "connection attempt");
        Thread connectThread = new Thread() {
            public void run() {
                // TODO: implement errors handling.
                while(!checkSessionReady()) {
                    while(!checkLoginReady()) {
                        // Login with credentials.
                        icqSession.clientLogin();
                    }
                    // Attempt to start session.
                    icqSession.startSession();
                }
                // Starting events fetching in verbal cycle.
                icqSession.startEventsFetching();
                // Update account connecting state to false.
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

    public void setClientLoginResult(String login, String tokenA, String sessionKey,
                                     int expiresIn, long hostTime) {
        // Setup local variables.
        this.tokenA = tokenA;
        this.sessionKey = sessionKey;
        this.timeDelta = hostTime - System.currentTimeMillis() / 1000;
        this.tokenExpirationDate = expiresIn + System.currentTimeMillis() / 1000;
        // Save account data in database.
        updateAccount();
    }

    public boolean checkLoginReady() {
        return !(TextUtils.isEmpty(tokenA) || TextUtils.isEmpty(sessionKey)
                || tokenExpirationDate == 0);
    }

    public long getHostTime() {
        return timeDelta + System.currentTimeMillis() / 1000;
    }

    public void setStartSessionResult(String aimSid, String fetchBaseURL,
                                      MyInfo myInfo, WellKnownUrls wellKnownUrls) {
        this.aimSid = aimSid;
        this.fetchBaseURL = fetchBaseURL;
        this.myInfo = myInfo;
        this.wellKnownUrls = wellKnownUrls;
        // Save account data in database.
        updateAccount();
    }

    public String getTokenA() {
        return tokenA;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public boolean checkSessionReady() {
        return !(TextUtils.isEmpty(aimSid) || TextUtils.isEmpty(fetchBaseURL)
                || myInfo == null || wellKnownUrls == null);
    }
}
