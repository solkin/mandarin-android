package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.CredentialsCheckCallback;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

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
    private String tokenCabbage;
    private String sessionKey;
    private long tokenExpirationDate;
    private long timeDelta;
    // Start session variables.
    private String aimSid;
    private String fetchBaseUrl;
    private MyInfo myInfo;
    private WellKnownUrls wellKnownUrls;

    public IcqAccountRoot() {
        icqSession = new IcqSession(this);
    }

    public IcqSession getSession() {
        return icqSession;
    }

    @Override
    public void connect() {
        Logger.log("icq connection attempt");
        // TODO: Such thread working model must be rewritten.
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
                    Logger.log("login: " + "start");
                    while (!checkSessionReady()) {
                        Logger.log("login: " + "session not ready");
                        while (!checkLoginReady()) {
                            Logger.log("login: " + "login not ready");
                            if (checkLoginExist()) {
                                Logger.log("login: " + "login exists, try to renew token");
                                // Try to renew token.
                                switch (icqSession.renewToken()) {
                                    case IcqSession.EXTERNAL_UNKNOWN: {
                                        Logger.log("login: " + "renew token external error");
                                        if (isPasswordLogin()) {
                                            Logger.log("login: " + "password login - reset login data");
                                            // Reset login data and try to client login.
                                            resetLoginData();
                                        } else {
                                            Logger.log("login: " + "can't renew token and no password :(");
                                            // Show notification.
                                            updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                        }
                                        break;
                                    }
                                    case IcqSession.INTERNAL_ERROR: {
                                        Logger.log("login: " + "renew token internal error");
                                        sleep();
                                        break;
                                    }
                                }
                            } else {
                                Logger.log("login: " + "no login data, lets client login");
                                // Login with credentials.
                                switch (icqSession.clientLogin()) {
                                    case IcqSession.EXTERNAL_LOGIN_ERROR: {
                                        Logger.log("login: " + "client login error");
                                        // Show notification.
                                        updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                        return;
                                    }
                                    case IcqSession.EXTERNAL_UNKNOWN: {
                                        Logger.log("login: " + "client login external unknown");
                                        // Show notification.
                                        updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                        return;
                                    }
                                    case IcqSession.INTERNAL_ERROR: {
                                        Logger.log("login: " + "client login internal error");
                                        // Sleep some time.
                                        sleep();
                                        break;
                                    }
                                }
                            }
                        }
                        Logger.log("login: " + "start session attempt");
                        // Attempt to start session.
                        switch (icqSession.startSession()) {
                            case IcqSession.EXTERNAL_SESSION_OK: {
                                Logger.log("login: " + "session started ok");
                                break;
                            }
                            case IcqSession.EXTERNAL_SESSION_RATE_LIMIT: {
                                Logger.log("login: " + "start session rate limit");
                                // Show notification.
                                updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                return;
                            }
                            case IcqSession.EXTERNAL_UNKNOWN: {
                                Logger.log("login: " + "start session external error");
                                // Renew token or retry client login.
                                expireLoginData();
                                break;
                            }
                            case IcqSession.INTERNAL_ERROR: {
                                Logger.log("login: " + "start session internal error");
                                // Sleep some time.
                                sleep();
                                break;
                            }
                        }
                    }
                    Logger.log("login: " + "session ready, almost ok");
                    // Update account connecting state to false.
                    updateAccountState(false);
                    // Starting events fetching in verbal cycle.
                } while (!icqSession.startEventsFetching());
                // Update offline status.
                updateAccountState(StatusUtil.STATUS_OFFLINE, false);
            }
        };
        connectThread.start();
    }

    @Override
    public void disconnect() {
        RequestHelper.endSession(getContentResolver(), accountDbId);
    }

    public boolean isPasswordLogin() {
        return !TextUtils.isEmpty(getUserPassword());
    }

    @Override
    public void checkCredentials(final CredentialsCheckCallback callback) {
        // TODO: Such thread working model must be rewritten.
        Thread credentialsCheckThread = new Thread() {

            private void sleep() {
                try {
                    sleep(5000);
                } catch (InterruptedException ignored) {
                    // No need to check.
                }
            }

            public void run() {
                while (!checkLoginReady()) {
                    switch (icqSession.clientLogin()) {
                        case IcqSession.EXTERNAL_LOGIN_ERROR:
                        case IcqSession.EXTERNAL_UNKNOWN: {
                            callback.onFailed();
                            return;
                        }
                        case IcqSession.INTERNAL_ERROR: {
                            // Sleep some time.
                            sleep();
                            break;
                        }
                    }
                }
                callback.onPassed();
            }
        };
        credentialsCheckThread.start();
    }

    public void updateStatus() {
        RequestHelper.requestSetState(getContentResolver(), getAccountDbId(),
                getBaseStatusValue(statusIndex));
        RequestHelper.requestSetMood(getContentResolver(), getAccountDbId(),
                getMoodStatusValue(statusIndex), statusTitle, statusMessage);
    }

    protected int getBaseStatusValue(int statusIndex) {
        int moodOffset = getStatusIndex(R.integer.mood_offset);
        // Checking for status type - base or mood.
        if (statusIndex >= moodOffset) {
            statusIndex = getStatusIndex(R.integer.default_base_status);
        }
        return statusIndex;
    }

    protected int getMoodStatusValue(int statusIndex) {
        int moodOffset = getStatusIndex(R.integer.mood_offset);
        // Checking for status type - base or mood.
        if (statusIndex < moodOffset) {
            statusIndex = SetMoodRequest.STATUS_MOOD_RESET;
        }
        return statusIndex;
    }

    private int getStatusIndex(int resourceId) {
        return getContext().getResources().getInteger(resourceId);
    }

    @Override
    public String getAccountType() {
        return getClass().getName();
    }

    public static int getStatusNamesResource() {
        return R.array.status_names_icq;
    }

    public static int getStatusDrawablesResource() {
        return R.array.status_drawable_icq;
    }

    public static int getStatusValuesResource() {
        return R.array.status_values_icq;
    }

    public static int getStatusConnectResource() {
        return R.array.status_connect_icq;
    }

    public static int getStatusSetupResource() {
        return R.array.status_setup_icq;
    }

    public static int getStatusMusicResource() {
        return R.integer.music_status;
    }

    public void setClientLoginResult(String login, String tokenA, String sessionKey,
                                     long expiresIn, long hostTime) {
        // Setup local variables.
        this.userId = login;
        this.tokenA = tokenA;
        this.sessionKey = sessionKey;
        this.timeDelta = hostTime - System.currentTimeMillis() / 1000;
        this.tokenExpirationDate = expiresIn + System.currentTimeMillis() / 1000;
        // Save account data in database.
        updateAccount();
    }

    public void setStartSessionResult(String aimSid, String fetchBaseUrl, WellKnownUrls wellKnownUrls) {
        this.aimSid = aimSid;
        this.fetchBaseUrl = fetchBaseUrl;
        this.wellKnownUrls = wellKnownUrls;
    }

    public void setRenewTokenResult(String login, String tokenA, long expiresIn) {
        // Setup local variables.
        this.tokenA = tokenA;
        this.tokenExpirationDate = expiresIn + System.currentTimeMillis() / 1000;
        // Save account data in database.
        updateAccount();
    }

    /**
     * Updates account brief and status information. Also, updates account info in database.
     *
     * @param myInfo - protocol-based object with basic account info.
     */
    protected void setMyInfo(MyInfo myInfo) {
        this.myInfo = myInfo;

        setUserNick(myInfo.getFriendly());

        // Avatar checking and requesting.
        String buddyIcon = myInfo.getBuddyIcon();
        String bigBuddyIcon = myInfo.getBigBuddyIcon();
        if (!TextUtils.isEmpty(bigBuddyIcon)) {
            buddyIcon = bigBuddyIcon;
        }
        if (TextUtils.isEmpty(buddyIcon)) {
            setAvatarHash(null);
        } else {
            if (!TextUtils.equals(getAvatarHash(), HttpUtil.getUrlHash(buddyIcon))) {
                // Avatar is ready.
                RequestHelper.requestAccountAvatar(getContentResolver(),
                        getAccountDbId(), buddyIcon);
            }
        }

        // Update account status info.
        String buddyStatus = myInfo.getState();
        String moodIcon = myInfo.optMoodIcon();
        String statusMessage = myInfo.optStatusMsg();
        String moodTitle = myInfo.optMoodTitle();

        int statusIndex = icqSession.getStatusIndex(moodIcon, buddyStatus);
        String statusTitle = icqSession.getStatusTitle(moodTitle, statusIndex);

        // Checking for we are disconnecting now.
        if (getStatusIndex() != StatusUtil.STATUS_OFFLINE) {
            // This will update account state and write account into db.
            updateAccountState(statusIndex, statusTitle, statusMessage, false);
        }
    }

    public boolean checkLoginExpired() {
        return System.currentTimeMillis() / 1000 > tokenExpirationDate;
    }

    public boolean checkLoginExist() {
        return !TextUtils.isEmpty(tokenA) && !TextUtils.isEmpty(sessionKey);
    }

    public boolean checkLoginReady() {
        return !(TextUtils.isEmpty(tokenA) || TextUtils.isEmpty(sessionKey)
                || checkLoginExpired());
    }

    public boolean checkSessionReady() {
        return !(TextUtils.isEmpty(aimSid) || TextUtils.isEmpty(fetchBaseUrl)
                || myInfo == null || wellKnownUrls == null);
    }

    public void expireLoginData() {
        tokenExpirationDate = 0;
    }

    public void resetLoginData() {
        tokenA = null;
        sessionKey = null;
        tokenExpirationDate = 0;
    }

    public void resetSessionData() {
        aimSid = null;
        fetchBaseUrl = null;
        wellKnownUrls = null;
    }

    public long getHostTime() {
        return timeDelta + System.currentTimeMillis() / 1000;
    }

    public void setHostTime(long hostTime) {
        this.timeDelta = hostTime - System.currentTimeMillis() / 1000;
    }

    public String getTokenA() {
        return tokenA;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getTokenCabbage() {
        return tokenCabbage;
    }

    public String getAimSid() {
        return aimSid;
    }

    public String getFetchBaseUrl() {
        return fetchBaseUrl;
    }

    public void setFetchBaseUrl(String fetchBaseUrl) {
        this.fetchBaseUrl = fetchBaseUrl;
    }

    public MyInfo getMyInfo() {
        return myInfo;
    }

    public WellKnownUrls getWellKnownUrls() {
        return wellKnownUrls;
    }
}
