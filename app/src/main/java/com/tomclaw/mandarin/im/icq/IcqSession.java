package com.tomclaw.mandarin.im.icq;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;

import com.tomclaw.mandarin.BuildConfig;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.BuddyData;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.GroupData;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.dto.HistDlgState;
import com.tomclaw.mandarin.im.icq.dto.Message;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.NameValuePair;
import com.tomclaw.mandarin.util.StringUtil;
import com.tomclaw.mandarin.util.UrlParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.tomclaw.mandarin.im.icq.WimConstants.AIM_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.AIM_SID;
import static com.tomclaw.mandarin.im.icq.WimConstants.AMP;
import static com.tomclaw.mandarin.im.icq.WimConstants.ASSERT_CAPS;
import static com.tomclaw.mandarin.im.icq.WimConstants.BUDDIES_ARRAY;
import static com.tomclaw.mandarin.im.icq.WimConstants.BUDDYLIST;
import static com.tomclaw.mandarin.im.icq.WimConstants.BUDDY_ICON;
import static com.tomclaw.mandarin.im.icq.WimConstants.BUILD_NUMBER;
import static com.tomclaw.mandarin.im.icq.WimConstants.CLIENT_LOGIN_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.CLIENT_NAME;
import static com.tomclaw.mandarin.im.icq.WimConstants.CLIENT_VERSION;
import static com.tomclaw.mandarin.im.icq.WimConstants.DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.DEVICE_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.DEV_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.DEV_ID_K;
import static com.tomclaw.mandarin.im.icq.WimConstants.DISPLAY_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.EQUAL;
import static com.tomclaw.mandarin.im.icq.WimConstants.EVENTS;
import static com.tomclaw.mandarin.im.icq.WimConstants.EVENTS_ARRAY;
import static com.tomclaw.mandarin.im.icq.WimConstants.EVENT_DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.EXPIRES_IN;
import static com.tomclaw.mandarin.im.icq.WimConstants.FETCH_BASE_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.FORMAT;
import static com.tomclaw.mandarin.im.icq.WimConstants.FRIENDLY;
import static com.tomclaw.mandarin.im.icq.WimConstants.GROUPS_ARRAY;
import static com.tomclaw.mandarin.im.icq.WimConstants.HIST_DLG_STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.HOST_TIME;
import static com.tomclaw.mandarin.im.icq.WimConstants.ID_FIELD;
import static com.tomclaw.mandarin.im.icq.WimConstants.ID_TYPE;
import static com.tomclaw.mandarin.im.icq.WimConstants.IMF;
import static com.tomclaw.mandarin.im.icq.WimConstants.IM_STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.IM_STATES;
import static com.tomclaw.mandarin.im.icq.WimConstants.IM_STATES_ARRAY;
import static com.tomclaw.mandarin.im.icq.WimConstants.INCLUDE_PRESENCE_FIELDS;
import static com.tomclaw.mandarin.im.icq.WimConstants.INVISIBLE;
import static com.tomclaw.mandarin.im.icq.WimConstants.LANGUAGE;
import static com.tomclaw.mandarin.im.icq.WimConstants.LAST_SEEN;
import static com.tomclaw.mandarin.im.icq.WimConstants.LOGIN;
import static com.tomclaw.mandarin.im.icq.WimConstants.LOGIN_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.MINIMIZE_RESPONSE;
import static com.tomclaw.mandarin.im.icq.WimConstants.MOBILE;
import static com.tomclaw.mandarin.im.icq.WimConstants.MOOD_ICON;
import static com.tomclaw.mandarin.im.icq.WimConstants.MOOD_TITLE;
import static com.tomclaw.mandarin.im.icq.WimConstants.MSG_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.MY_INFO;
import static com.tomclaw.mandarin.im.icq.WimConstants.NAME;
import static com.tomclaw.mandarin.im.icq.WimConstants.PASSWORD;
import static com.tomclaw.mandarin.im.icq.WimConstants.PEEK;
import static com.tomclaw.mandarin.im.icq.WimConstants.POLL_TIMEOUT;
import static com.tomclaw.mandarin.im.icq.WimConstants.POST_PREFIX;
import static com.tomclaw.mandarin.im.icq.WimConstants.PRESENCE;
import static com.tomclaw.mandarin.im.icq.WimConstants.RAW_MSG;
import static com.tomclaw.mandarin.im.icq.WimConstants.RENEW_TOKEN;
import static com.tomclaw.mandarin.im.icq.WimConstants.RENEW_TOKEN_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.R_PARAM;
import static com.tomclaw.mandarin.im.icq.WimConstants.SEND_REQ_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.SESSION_ENDED;
import static com.tomclaw.mandarin.im.icq.WimConstants.SESSION_KEY;
import static com.tomclaw.mandarin.im.icq.WimConstants.SESSION_SECRET;
import static com.tomclaw.mandarin.im.icq.WimConstants.SESSION_TIMEOUT;
import static com.tomclaw.mandarin.im.icq.WimConstants.SIG_SHA256;
import static com.tomclaw.mandarin.im.icq.WimConstants.START_SESSION_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_MSG;
import static com.tomclaw.mandarin.im.icq.WimConstants.TIMEOUT;
import static com.tomclaw.mandarin.im.icq.WimConstants.TOKEN_A;
import static com.tomclaw.mandarin.im.icq.WimConstants.TOKEN_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.TS;
import static com.tomclaw.mandarin.im.icq.WimConstants.TYPE;
import static com.tomclaw.mandarin.im.icq.WimConstants.TYPING;
import static com.tomclaw.mandarin.im.icq.WimConstants.TYPING_STATUS;
import static com.tomclaw.mandarin.im.icq.WimConstants.TYPING_STATUS_TYPE;
import static com.tomclaw.mandarin.im.icq.WimConstants.URL_REGEX;
import static com.tomclaw.mandarin.im.icq.WimConstants.USER_DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.USER_TYPE;
import static com.tomclaw.mandarin.im.icq.WimConstants.VIEW;
import static com.tomclaw.mandarin.im.icq.WimConstants.WELL_KNOWN_URLS;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:20 PM
 */
public class IcqSession {

    public static final String DEV_ID_VALUE = "ic12G5kB_856lXr1";
    private static final String EVENTS_VALUE = "myInfo,presence,buddylist,typing,imState,userAddedToBuddyList,service,buddyRegistered,hist";
    private static final String PRESENCE_FIELDS_VALUE = "userType,service,moodIcon,moodTitle,capabilities,aimId,displayId,friendly,state,buddyIcon,bigBuddyIcon,abPhones,smsNumber,statusMsg,seqNum,eventType,lastseen";
    private static final String CLIENT_NAME_VALUE = "Mandarin%20Android";
    private static final String CLIENT_VERSION_VALUE = BuildConfig.VERSION_NAME;
    private static final String BUILD_NUMBER_VALUE = String.valueOf(BuildConfig.VERSION_CODE);
    private static final String ASSERT_CAPS_VALUE = "4d616e646172696e20494d0003000000,094613544C7F11D18222444553540000";
    private static final String DEVICE_ID_VALUE = "mandarin_device_id";

    public static final int INTERNAL_ERROR = 1000;
    public static final int EXTERNAL_LOGIN_OK = 200;
    public static final int EXTERNAL_LOGIN_ERROR = 330;
    public static final int EXTERNAL_UNKNOWN = 0;
    public static final int EXTERNAL_SESSION_OK = 200;
    public static final int EXTERNAL_SESSION_RATE_LIMIT = 607;
    private static final int EXTERNAL_FETCH_OK = 200;

    private static final int TIMEOUT_SOCKET_ADDITION = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int TIMEOUT_CONNECTION = (int) TimeUnit.MINUTES.toMillis(2);
    private static final int TIMEOUT_SESSION = (int) TimeUnit.DAYS.toMillis(1);

    private IcqAccountRoot icqAccountRoot;

    private CabbageSession cabbageSession;

    public IcqSession(IcqAccountRoot icqAccountRoot) {
        this.icqAccountRoot = icqAccountRoot;
        cabbageSession = new CabbageSession(icqAccountRoot);
    }

    public int clientLogin() {
        try {
            // Create and config connection
            URL url = new URL(CLIENT_LOGIN_URL);
            HttpURLConnection loginConnection = (HttpURLConnection) url.openConnection();
            loginConnection.setConnectTimeout(TIMEOUT_CONNECTION);
            loginConnection.setReadTimeout(TIMEOUT_CONNECTION + TIMEOUT_SOCKET_ADDITION);

            Logger.log("timeout connection: " + TIMEOUT_CONNECTION);
            Logger.log("timeout session: " + TIMEOUT_SESSION);

            // Specifying login data.
            HttpParamsBuilder nameValuePairs = new HttpParamsBuilder()
                    .appendParam(CLIENT_NAME, CLIENT_NAME_VALUE)
                    .appendParam(CLIENT_VERSION, CLIENT_VERSION_VALUE)
                    .appendParam(DEV_ID, DEV_ID_VALUE)
                    .appendParam(FORMAT, WimConstants.FORMAT_JSON)
                    .appendParam(ID_TYPE, "ICQ")
                    .appendParam(PASSWORD, icqAccountRoot.getUserPassword())
                    .appendParam(LOGIN, icqAccountRoot.getUserId());

            try {
                // Execute request.
                InputStream responseStream = HttpUtil.executePost(loginConnection, nameValuePairs.build());
                String responseString = HttpUtil.streamToString(responseStream);
                responseStream.close();
                Logger.log("client login = " + responseString);

                JSONObject jsonObject = new JSONObject(responseString);
                JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
                int statusCode = responseObject.getInt(STATUS_CODE);
                switch (statusCode) {
                    case EXTERNAL_LOGIN_OK: {
                        JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                        String login = dataObject.getString(LOGIN_ID);
                        long hostTime = dataObject.getLong(HOST_TIME);
                        String sessionSecret = dataObject.getString(SESSION_SECRET);
                        JSONObject tokenObject = dataObject.getJSONObject(TOKEN_OBJECT);
                        int expiresIn = tokenObject.getInt(EXPIRES_IN);
                        String tokenA = tokenObject.getString(TOKEN_A);
                        Logger.log("token a = " + tokenA);
                        Logger.log("sessionSecret = " + sessionSecret);
                        String sessionKey = StringUtil.getHmacSha256Base64(sessionSecret, icqAccountRoot.getUserPassword());
                        Logger.log("sessionKey = " + sessionKey);
                        // Update client login result in database.
                        icqAccountRoot.setClientLoginResult(login, tokenA, sessionKey, expiresIn, hostTime);
                        return EXTERNAL_LOGIN_OK;
                    }
                    case EXTERNAL_LOGIN_ERROR: {
                        return EXTERNAL_LOGIN_ERROR;
                    }
                    default: {
                        return EXTERNAL_UNKNOWN;
                    }
                }
            } finally {
                loginConnection.disconnect();
            }
        } catch (Throwable e) {
            Logger.log("client login: " + e.getMessage());
            return INTERNAL_ERROR;
        }
    }

    public int startSession() {
        try {
            URL url = new URL(START_SESSION_URL);
            HttpURLConnection startSessionConnection = (HttpURLConnection) url.openConnection();
            startSessionConnection.setConnectTimeout(TIMEOUT_CONNECTION);
            startSessionConnection.setReadTimeout(TIMEOUT_CONNECTION + TIMEOUT_SOCKET_ADDITION);

            String statusValue = StatusUtil.getStatusValue(icqAccountRoot.getAccountType(),
                    icqAccountRoot.getBaseStatusValue(icqAccountRoot.getStatusIndex()));
            // Add your data
            HttpParamsBuilder nameValuePairs = new HttpParamsBuilder()
                    .appendParam(WimConstants.TOKEN_A, icqAccountRoot.getTokenA())
                    .appendParam(ASSERT_CAPS, ASSERT_CAPS_VALUE)
                    .appendParam(BUILD_NUMBER, BUILD_NUMBER_VALUE)
                    .appendParam(CLIENT_NAME, CLIENT_NAME_VALUE)
                    .appendParam(CLIENT_VERSION, CLIENT_VERSION_VALUE)
                    .appendParam(DEVICE_ID, DEVICE_ID_VALUE)
                    .appendParam(EVENTS, EVENTS_VALUE)
                    .appendParam(FORMAT, WimConstants.FORMAT_JSON)
                    .appendParam(IMF, "plain")
                    .appendParam(INCLUDE_PRESENCE_FIELDS, PRESENCE_FIELDS_VALUE)
                    .appendParam(INVISIBLE, "false")
                    .appendParam(DEV_ID_K, DEV_ID_VALUE)
                    .appendParam(LANGUAGE, "ru-ru")
                    .appendParam(MINIMIZE_RESPONSE, "0")
                    .appendParam(MOBILE, "0")
                    .appendParam(POLL_TIMEOUT, String.valueOf(TIMEOUT_CONNECTION))
                    .appendParam(RAW_MSG, "0")
                    .appendParam(SESSION_TIMEOUT, String.valueOf(TIMEOUT_SESSION / 1000))
                    .appendParam(TS, String.valueOf(icqAccountRoot.getHostTime()))
                    .appendParam(VIEW, statusValue);

            String hash = POST_PREFIX.concat(URLEncoder.encode(START_SESSION_URL, HttpUtil.UTF8_ENCODING))
                    .concat(AMP).concat(URLEncoder.encode(nameValuePairs.build(), HttpUtil.UTF8_ENCODING));

            nameValuePairs.appendParam(SIG_SHA256,
                    StringUtil.getHmacSha256Base64(hash, icqAccountRoot.getSessionKey()));
            Logger.log(nameValuePairs.build());
            try {
                // Execute HTTP Post Request
                InputStream responseStream = HttpUtil.executePost(startSessionConnection, nameValuePairs.build());
                String responseString = HttpUtil.streamToString(responseStream);
                responseStream.close();
                Logger.log("start session = " + responseString);

                JSONObject jsonObject = new JSONObject(responseString);
                JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
                int statusCode = responseObject.getInt(STATUS_CODE);
                switch (statusCode) {
                    case EXTERNAL_SESSION_OK: {
                        JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                        String aimSid = dataObject.getString(AIM_SID);
                        String fetchBaseUrl = dataObject.getString(FETCH_BASE_URL);
                        // Parsing my info and well-known URL's to send requests.
                        MyInfo myInfo = GsonSingleton.getInstance().fromJson(
                                StringUtil.fixCyrillicSymbols(dataObject.getJSONObject(MY_INFO).toString()), MyInfo.class);
                        WellKnownUrls wellKnownUrls = GsonSingleton.getInstance().fromJson(
                                dataObject.getJSONObject(WELL_KNOWN_URLS).toString(), WellKnownUrls.class);
                        // Update starts session result in database.
                        icqAccountRoot.setStartSessionResult(aimSid, fetchBaseUrl, wellKnownUrls);
                        // Request for status update before my info parsing to prevent status reset.
                        icqAccountRoot.updateStatus();
                        // Update status info in my info to prevent status blinking.
                        myInfo.setState(StatusUtil.getStatusValue(icqAccountRoot.getAccountType(),
                                icqAccountRoot.getBaseStatusValue(icqAccountRoot.getStatusIndex())));
                        int moodStatusValue = icqAccountRoot.getMoodStatusValue(icqAccountRoot.getStatusIndex());
                        if (moodStatusValue == SetMoodRequest.STATUS_MOOD_RESET) {
                            myInfo.setMoodIcon(null);
                        } else {
                            myInfo.setMoodIcon(StatusUtil.getStatusValue(icqAccountRoot.getAccountType(),
                                    icqAccountRoot.getMoodStatusValue(icqAccountRoot.getStatusIndex())));
                        }
                        myInfo.setMoodTitle(icqAccountRoot.getStatusTitle());
                        myInfo.setStatusMsg(icqAccountRoot.getStatusMessage());
                        // Now we can update info.
                        icqAccountRoot.setMyInfo(myInfo);
                        return EXTERNAL_SESSION_OK;
                    }
                    case EXTERNAL_SESSION_RATE_LIMIT: {
                        return EXTERNAL_SESSION_RATE_LIMIT;
                    }
                    // TODO: may be cases if ts incorrect. May be proceed too.
                    default: {
                        return EXTERNAL_UNKNOWN;
                    }
                }
            } finally {
                startSessionConnection.disconnect();
            }
        } catch (Throwable ex) {
            Logger.log("start session exception", ex);
            return INTERNAL_ERROR;
        }
    }

    public int renewToken() {
        try {
            HttpParamsBuilder builder = new HttpParamsBuilder();
            builder.appendParam(TOKEN_A, icqAccountRoot.getTokenA());
            builder.appendParam(SESSION_KEY, icqAccountRoot.getSessionKey());
            builder.appendParam(RENEW_TOKEN, "1");

            String url = signRequest(HttpUtil.GET, RENEW_TOKEN_URL, false, builder);

            Logger.log("renew token request: " + url);

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));
            Logger.log("renew token response: " + response);
            JSONObject jsonObject = new JSONObject(response);
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            switch (statusCode) {
                case EXTERNAL_LOGIN_OK: {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    JSONObject userDataObject = dataObject.getJSONObject(USER_DATA_OBJECT);
                    String login = userDataObject.getString(LOGIN_ID);
                    Logger.log("renew token login = " + login);
                    JSONObject tokenObject = dataObject.getJSONObject(TOKEN_OBJECT);
                    int expiresIn = tokenObject.getInt(EXPIRES_IN);
                    String tokenA = tokenObject.getString(TOKEN_A);
                    Logger.log("renew token expires in = " + expiresIn);
                    Logger.log("renew token token a = " + tokenA);
                    // Update renew token result in database.
                    icqAccountRoot.setRenewTokenResult(login, tokenA, expiresIn);
                    break;
                }
                default: {
                    return EXTERNAL_UNKNOWN;
                }
            }
            return EXTERNAL_LOGIN_OK;
        } catch (Throwable ex) {
            Logger.log("renew token exception", ex);
            return INTERNAL_ERROR;
        }
    }

    /**
     * Start event fetching in verbal cycle.
     *
     * @return true if we are now in offline mode because of user decision.
     * false if our session is not accepted by the server.
     */
    public boolean startEventsFetching() {
        cabbageSession.obtainToken();
        cabbageSession.obtainClient();
        cabbageSession.refreshClient();
        Logger.log("start events fetching");
        do {
            try {
                String fetchUrl = getFetchUrl();
                URL url = new URL(fetchUrl);
                Logger.log("fetch url = " + fetchUrl);
                HttpURLConnection fetchEventConnection = (HttpURLConnection) url.openConnection();
                fetchEventConnection.setConnectTimeout(TIMEOUT_CONNECTION);
                fetchEventConnection.setReadTimeout(TIMEOUT_CONNECTION + TIMEOUT_SOCKET_ADDITION);
                try {
                    InputStream responseStream = HttpUtil.executeGet(fetchEventConnection);
                    String responseString = HttpUtil.streamToString(responseStream);
                    responseStream.close();
                    Logger.log("fetch events = " + responseString);

                    JSONObject jsonObject = new JSONObject(responseString);
                    JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
                    int statusCode = responseObject.getInt(STATUS_CODE);
                    switch (statusCode) {
                        case EXTERNAL_FETCH_OK: {
                            JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                            long hostTime = dataObject.optLong(TS);
                            if (hostTime != 0) {
                                // Update time and fetch base url.
                                icqAccountRoot.setHostTime(hostTime);
                            }
                            String fetchBaseUrl = dataObject.optString(FETCH_BASE_URL);
                            if (!TextUtils.isEmpty(fetchBaseUrl)) {
                                icqAccountRoot.setFetchBaseUrl(fetchBaseUrl);
                            }
                            // Store account state.
                            icqAccountRoot.updateAccount();
                            // Process events.
                            JSONArray eventsArray = dataObject.getJSONArray(EVENTS_ARRAY);
                            // Cycling all events.
                            Logger.log("Cycling all events.");
                            for (int c = 0; c < eventsArray.length(); c++) {
                                JSONObject eventObject = eventsArray.getJSONObject(c);
                                String eventType = eventObject.getString(TYPE);
                                JSONObject eventData = eventObject.getJSONObject(EVENT_DATA_OBJECT);
                                // Process event.
                                processEvent(eventType, eventData);
                            }
                            break;
                        }
                        default: {
                            // Something wend wrong. Let's reconnect if status is not offline.
                            // Reset login and session data.
                            Logger.log("Something wend wrong. Let's reconnect if status is not offline.");
                            icqAccountRoot.resetSessionData();
                            icqAccountRoot.updateAccount();
                            return icqAccountRoot.getStatusIndex() == StatusUtil.STATUS_OFFLINE;
                        }
                    }
                } finally {
                    fetchEventConnection.disconnect();
                }
            } catch (Throwable ex) {
                Logger.log("fetch events exception: " + ex.getMessage());
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException ignored) {
                    // We'll sleep while there is no network connection.
                }
            }
        } while (!icqAccountRoot.isOffline()); // Fetching until online.
        return true;
    }

    public String getFetchUrl() {
        return new StringBuilder()
                .append(icqAccountRoot.getFetchBaseUrl())
                .append(AMP).append(FORMAT).append(EQUAL).append(WimConstants.FORMAT_JSON)
                .append(AMP).append(TIMEOUT).append(EQUAL).append(TIMEOUT_CONNECTION)
                .append(AMP).append(R_PARAM).append(EQUAL).append(System.currentTimeMillis())
                .append(AMP).append(PEEK).append(EQUAL).append(0).toString();
    }

    private void processEvent(String eventType, JSONObject eventData) {
        Logger.log("eventType = " + eventType + "; eventData = " + eventData.toString());
        long processStartTime = System.currentTimeMillis();
        switch (eventType) {
            case BUDDYLIST:
                try {
                    ArrayList<GroupData> groupDatas = new ArrayList<>();
                    int accountDbId = icqAccountRoot.getAccountDbId();
                    String accountType = icqAccountRoot.getAccountType();
                    ContentResolver contentResolver = icqAccountRoot.getContentResolver();
                    JSONArray groupsArray = eventData.getJSONArray(GROUPS_ARRAY);
                    for (int c = 0; c < groupsArray.length(); c++) {
                        JSONObject groupObject = groupsArray.getJSONObject(c);
                        String groupName = groupObject.getString(NAME);
                        int groupId = groupObject.getInt(ID_FIELD);
                        JSONArray buddiesArray = groupObject.getJSONArray(BUDDIES_ARRAY);
                        ArrayList<BuddyData> buddyDatas = new ArrayList<>();
                        for (int i = 0; i < buddiesArray.length(); i++) {
                            JSONObject buddyObject = buddiesArray.getJSONObject(i);
                            String buddyId = buddyObject.getString(AIM_ID);
                            String buddyNick = buddyObject.optString(FRIENDLY);
                            if (TextUtils.isEmpty(buddyNick)) {
                                buddyNick = buddyObject.optString(DISPLAY_ID, buddyId);
                            }
                            String buddyStatus = buddyObject.getString(STATE);
                            String moodIcon = buddyObject.optString(MOOD_ICON);
                            String statusMessage = buddyObject.optString(STATUS_MSG);
                            String moodTitle = buddyObject.optString(MOOD_TITLE);
                            int statusIndex = getStatusIndex(moodIcon, buddyStatus);
                            String statusTitle = getStatusTitle(moodTitle, statusIndex);
                            String buddyType = buddyObject.getString(USER_TYPE);
                            String buddyIcon = buddyObject.optString(BUDDY_ICON);
                            String bigBuddyIcon = buddyObject.optString(WimConstants.BIG_BUDDY_ICON);
                            if (!TextUtils.isEmpty(bigBuddyIcon)) {
                                buddyIcon = bigBuddyIcon;
                            }
                            long lastSeen = buddyObject.optLong(LAST_SEEN, -1);
                            buddyDatas.add(new BuddyData(groupId, groupName, buddyId, buddyNick, statusIndex,
                                    statusTitle, statusMessage, buddyIcon, lastSeen));
                        }
                        groupDatas.add(new GroupData(groupName, groupId, buddyDatas));
                    }
                    // Prepare parameters to call update roster method.
                    Bundle bundle = new Bundle();
                    bundle.putInt(GlobalProvider.KEY_ACCOUNT_DB_ID, accountDbId);
                    bundle.putString(GlobalProvider.KEY_ACCOUNT_TYPE, accountType);
                    bundle.putSerializable(GlobalProvider.KEY_GROUP_DATAS, groupDatas);
                    contentResolver.call(Settings.BUDDY_RESOLVER_URI, GlobalProvider.METHOD_UPDATE_ROSTER, null, bundle);
                } catch (JSONException ex) {
                    Logger.log("exception while parsing buddy list", ex);
                }
                break;
            case HIST_DLG_STATE:
                GsonSingleton gson = GsonSingleton.getInstance();
                try {
                    HistDlgState histDlgState = gson.fromJson(eventData.toString(), HistDlgState.class);

                    for (Message message : histDlgState.getMessages()) {
                        boolean isProcessed = false;
                        do {
                            try {
                                Matcher matcher = URL_REGEX.matcher(message.getText());
                                while (matcher.find() && matcher.groupCount() == 1) {
                                    // TODO: also show message body.
                                    String url = matcher.group();
                                    String fileId = matcher.group(1);
                                    int buddyDbId = QueryHelper.getBuddyDbId(icqAccountRoot.getContentResolver(),
                                            icqAccountRoot.getAccountDbId(), histDlgState.getSn());
                                    String tag = message.getMsgId() + ":" + url;
                                    RequestHelper.requestFileReceive(icqAccountRoot.getContentResolver(),
                                            buddyDbId, String.valueOf(message.getMsgId()), message.getTime() * 1000, fileId, url, message.getText(), tag);
                                    isProcessed = true;
                                }
                                if (!isProcessed) {
                                    int messageType = message.isOutgoing() ? GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING : GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
//                                    QueryHelper.insertMessage(icqAccountRoot.getContentResolver(),
//                                            icqAccountRoot.getAccountDbId(), histDlgState.getSn(),
//                                            messageType, String.valueOf(message.getMsgId()),
//                                            message.getTime() * 1000, message.getText());
                                }
                                isProcessed = true;
                            } catch (BuddyNotFoundException ignored) {
                                if (PreferenceHelper.isIgnoreUnknown(icqAccountRoot.getContext())) {
                                    isProcessed = true;
                                } else {
                                    int statusIndex = StatusUtil.STATUS_OFFLINE;
                                    String statusTitle = getStatusTitle(null, statusIndex);
                                    String buddyNick = histDlgState.getPersons().get(0).getFriendly();
                                    String statusMessage = "";
                                    String buddyIcon = null;
                                    long lastSeen = -1;

                                    String recycleString = icqAccountRoot.getResources().getString(R.string.recycle);
                                    QueryHelper.updateOrCreateBuddy(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                                            icqAccountRoot.getAccountType(), System.currentTimeMillis(), GlobalProvider.GROUP_ID_RECYCLE,
                                            recycleString, histDlgState.getSn(), buddyNick, statusIndex, statusTitle, statusMessage, buddyIcon, lastSeen);
                                }
                            }
                            // This will try to create buddy if such is not present
                            // in roster and then retry message insertion.
                        } while (!isProcessed);
                    }
                } catch (Throwable ex) {
                    Logger.log("exception while parsing history dialog state", ex);
                }
                break;
            case IM_STATE:
                try {
                    JSONArray imStatesArray = eventData.getJSONArray(IM_STATES_ARRAY);
                    for (int c = 0; c < imStatesArray.length(); c++) {
                        JSONObject imState = imStatesArray.getJSONObject(c);
                        String state = imState.getString(STATE);
                        String msgId = imState.getString(MSG_ID);
                        String sendReqId = imState.optString(SEND_REQ_ID);
                        for (int i = 0; i < IM_STATES.length; i++) {
                            if (state.equals(IM_STATES[i])) {
                                // TODO: something to do maybe?
                                break;
                            }
                        }
                    }
                } catch (JSONException ex) {
                    Logger.log("error while processing im state", ex);
                }
                break;
            case PRESENCE:
                try {
                    String buddyId = eventData.getString(AIM_ID);

                    String buddyStatus = eventData.getString(STATE);
                    String moodIcon = eventData.optString(MOOD_ICON);
                    String statusMessage = StringUtil.unescapeXml(eventData.optString(STATUS_MSG));
                    String moodTitle = StringUtil.unescapeXml(eventData.optString(MOOD_TITLE));

                    int statusIndex = getStatusIndex(moodIcon, buddyStatus);
                    String statusTitle = getStatusTitle(moodTitle, statusIndex);

                    String buddyIcon = eventData.optString(BUDDY_ICON);
                    String bigBuddyIcon = eventData.optString(WimConstants.BIG_BUDDY_ICON);
                    if (!TextUtils.isEmpty(bigBuddyIcon)) {
                        buddyIcon = bigBuddyIcon;
                    }

                    long lastSeen = eventData.optLong(LAST_SEEN, -1);

                    QueryHelper.modifyBuddyStatus(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                            buddyId, statusIndex, statusTitle, statusMessage, buddyIcon, lastSeen);
                } catch (JSONException ex) {
                    Logger.log("error while processing presence - JSON exception", ex);
                } catch (BuddyNotFoundException ex) {
                    Logger.log("error while processing presence - buddy not found");
                }
                break;
            case TYPING:
                try {
                    String buddyId = eventData.getString(AIM_ID);
                    String typingStatus = eventData.getString(TYPING_STATUS);
                    QueryHelper.modifyBuddyTyping(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                            buddyId, TextUtils.equals(typingStatus, TYPING_STATUS_TYPE));
                } catch (Throwable ex) {
                    Logger.log("error while processing typing", ex);
                }
                break;
            case MY_INFO:
                try {
                    MyInfo myInfo = GsonSingleton.getInstance().fromJson(
                            StringUtil.fixCyrillicSymbols(eventData.toString()), MyInfo.class);
                    icqAccountRoot.setMyInfo(myInfo);
                } catch (Throwable ex) {
                    Logger.log("error while processing my info", ex);
                }
                break;
            case SESSION_ENDED:
                icqAccountRoot.resetSessionData();
                icqAccountRoot.carriedOff();
                break;
        }
        Logger.log("processed in " + (System.currentTimeMillis() - processStartTime) + " ms.");
    }

    protected String getStatusTitle(String moodTitle, int statusIndex) {
        // Define status title.
        String statusTitle;
        if (TextUtils.isEmpty(moodTitle)) {
            // Default title for status index.
            statusTitle = StatusUtil.getStatusTitle(icqAccountRoot.getAccountType(), statusIndex);
        } else {
            // Buddy specified title.
            statusTitle = moodTitle;
        }
        return statusTitle;
    }

    protected int getStatusIndex(String moodIcon, String buddyStatus) {
        int statusIndex;
        // Checking for mood present.
        if (!TextUtils.isEmpty(moodIcon)) {
            try {
                return StatusUtil.getStatusIndex(icqAccountRoot.getAccountType(), parseMood(moodIcon));
            } catch (StatusNotFoundException ignored) {
            }
        }
        try {
            statusIndex = StatusUtil.getStatusIndex(icqAccountRoot.getAccountType(), buddyStatus);
        } catch (StatusNotFoundException ex) {
            statusIndex = StatusUtil.STATUS_OFFLINE;
        }
        return statusIndex;
    }

    /**
     * Returns "id" parameter value from specified URL
     */
    private static String getIdParam(String url) {
        URI uri = URI.create(url);
        for (NameValuePair param : UrlParser.parse(uri, "UTF-8")) {
            if (param.getName().equals("id")) {
                return param.getValue();
            }
        }
        return "";
    }

    /**
     * Parsing specified URL for "id" parameter, decoding it from UTF-8 byte array in HEX presentation
     */
    public static String parseMood(String moodUrl) {
        if (moodUrl != null) {
            final String id = getIdParam(moodUrl);

            InputStream is = new InputStream() {
                int pos = 0;
                int length = id.length();

                @Override
                public int read() throws IOException {
                    if (pos == length) return -1;
                    char c1 = id.charAt(pos++);
                    char c2 = id.charAt(pos++);

                    return (Character.digit(c1, 16) << 4) | Character.digit(c2, 16);
                }
            };
            DataInputStream dis = new DataInputStream(is);
            try {
                return dis.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return moodUrl;
    }

    public String signRequest(String method, String url, HttpParamsBuilder builder)
            throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        return signRequest(method, url, true, builder);
    }

    public String signRequest(String method, String url, boolean includeSession, HttpParamsBuilder builder)
            throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        builder.appendParam(WimConstants.TOKEN_A, icqAccountRoot.getTokenA());
        if (includeSession) {
            builder.appendParam(WimConstants.AIM_SID, icqAccountRoot.getAimSid());
        }
        builder.appendParam(WimConstants.FORMAT, WimConstants.FORMAT_JSON)
                .appendParam(WimConstants.DEV_ID_K, DEV_ID_VALUE)
                .appendParam(WimConstants.TS, String.valueOf(System.currentTimeMillis() / 1000));
        builder.sortParams();
        String params = builder.build();
        String hash = method.concat(WimConstants.AMP).concat(StringUtil.urlEncode(url))
                .concat(WimConstants.AMP).concat(StringUtil.urlEncode(params));
        return url.concat(WimConstants.QUE).concat(params).concat(WimConstants.AMP)
                .concat(WimConstants.SIG_SHA256).concat(EQUAL)
                .concat(StringUtil.urlEncode(StringUtil.getHmacSha256Base64(hash, icqAccountRoot.getSessionKey())));
    }

    public void obtainCabbageToken() {
        cabbageSession.obtainToken();
        cabbageSession.obtainClient();
    }

    public void obtainCabbageClient() {
        cabbageSession.obtainClient();
    }
}