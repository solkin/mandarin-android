package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.HttpUtil;

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
        Log.d(Settings.LOG_TAG, "icq connection attempt");
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
                    while (!checkSessionReady()) {
                        while (!checkLoginReady()) {
                            // Login with credentials.
                            switch (icqSession.clientLogin()) {
                                case IcqSession.EXTERNAL_LOGIN_ERROR: {
                                    // Show notification.
                                    updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                    return;
                                }
                                case IcqSession.EXTERNAL_UNKNOWN: {
                                    // Show notification.
                                    updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                    return;
                                }
                                case IcqSession.INTERNAL_ERROR: {
                                    // Sleep some time.
                                    sleep();
                                    break;
                                }
                            }
                        }
                        // Attempt to start session.
                        switch (icqSession.startSession()) {
                            case IcqSession.EXTERNAL_SESSION_OK: {
                                break;
                            }
                            case IcqSession.EXTERNAL_SESSION_RATE_LIMIT: {
                                // Show notification.
                                updateAccountState(StatusUtil.STATUS_OFFLINE, false);
                                return;
                            }
                            case IcqSession.EXTERNAL_UNKNOWN: {
                                // Retry client login.
                                resetLoginData();
                                break;
                            }
                            case IcqSession.INTERNAL_ERROR: {
                                // Sleep some time.
                                sleep();
                                break;
                            }
                        }
                    }
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
        RequestHelper.endSession(getContentResolver(), CoreService.getAppSession(), accountDbId);
    }

    public void updateStatus() {
        RequestHelper.requestSetState(getContentResolver(), CoreService.getAppSession(), getAccountDbId(),
                getBaseStatusValue(statusIndex));
        RequestHelper.requestSetMood(getContentResolver(), CoreService.getAppSession(), getAccountDbId(),
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

    @Override
    public int getAccountLayout() {
        return R.layout.account_add_icq;
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

    public void setStartSessionResult(String aimSid, String fetchBaseUrl,
                                      MyInfo myInfo, WellKnownUrls wellKnownUrls) {
        this.aimSid = aimSid;
        this.fetchBaseUrl = fetchBaseUrl;
        this.wellKnownUrls = wellKnownUrls;
        setMyInfo(myInfo);
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
        if (TextUtils.isEmpty(buddyIcon)) {
            setAvatarHash(null);
        } else {
            if (!TextUtils.equals(getAvatarHash(), HttpUtil.getUrlHash(buddyIcon))) {
                // Avatar is ready.
                RequestHelper.requestAccountAvatar(getContentResolver(), CoreService.getAppSession(),
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

        // Heuristic check for invisible state.
        if (getStatusIndex() == getStatusIndex(R.integer.invisible_status)
                && statusIndex == getStatusIndex(R.integer.default_base_status)
                && TextUtils.equals(statusTitle, StatusUtil.getStatusTitle(getAccountType(), getStatusIndex(R.integer.invisible_status)))) {
            Log.d(Settings.LOG_TAG, "Invisible state detected!");
        } else if (getStatusIndex() != StatusUtil.STATUS_OFFLINE) { // Checking for we are disconnecting now.
            // This will update account state and write account into db.
            updateAccountState(statusIndex, statusTitle, statusMessage, false);
        }
    }

    public boolean checkLoginReady() {
        return !(TextUtils.isEmpty(tokenA) || TextUtils.isEmpty(sessionKey)
                || tokenExpirationDate == 0);
    }

    public boolean checkSessionReady() {
        return !(TextUtils.isEmpty(aimSid) || TextUtils.isEmpty(fetchBaseUrl)
                || myInfo == null || wellKnownUrls == null);
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
