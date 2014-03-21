package com.tomclaw.mandarin.im.icq;

import android.content.ContentResolver;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:20 PM
 */
public class IcqSession {

    private static final String DEV_ID_VALUE = "ic12G5kB_856lXr1";
    private static final String EVENTS_VALUE = "myInfo,presence,buddylist,typing,imState,im,sentIM,offlineIM,userAddedToBuddyList,service,buddyRegistered";
    private static final String PRESENCE_FIELDS_VALUE = "userType,service,moodIcon,moodTitle,capabilities,aimId,displayId,friendly,state,buddyIcon,abPhones,smsNumber,statusMsg,seqNum,eventType,lastseen";
    private static final String CLIENT_NAME_VALUE = "Mandarin%20Android";
    private static final String CLIENT_VERSION_VALUE = "1.0";
    private static final String BUILD_NUMBER_VALUE = "12";
    private static final String ASSERT_CAPS_VALUE = "4d616e646172696e20494d0003000000";
    private static final String DEVICE_ID_VALUE = "mandarin_device_id";

    public static final int INTERNAL_ERROR = 1000;
    public static final int EXTERNAL_LOGIN_OK = 200;
    public static final int EXTERNAL_LOGIN_ERROR = 330;
    public static final int EXTERNAL_UNKNOWN = 0;
    public static final int EXTERNAL_SESSION_OK = 200;
    public static final int EXTERNAL_SESSION_RATE_LIMIT = 607;
    private static final int EXTERNAL_FETCH_OK = 200;

    private static final int timeoutSocket = 70000;
    private static final int timeoutConnection = 60000;
    private static final int timeoutSession = 900000;

    private IcqAccountRoot icqAccountRoot;

    public IcqSession(IcqAccountRoot icqAccountRoot) {
        this.icqAccountRoot = icqAccountRoot;
    }

    public int clientLogin() {
        try {
            // Create and config connection
            URL url = new URL(CLIENT_LOGIN_URL);
            HttpURLConnection loginConnection = (HttpURLConnection) url.openConnection();
            loginConnection.setConnectTimeout(timeoutConnection);
            loginConnection.setReadTimeout(timeoutSocket);

            // Specifying login data.
            List<Pair<String, String>> nameValuePairs = new ArrayList<Pair<String, String>>();
            nameValuePairs.add(new Pair<String, String>(CLIENT_NAME, CLIENT_NAME_VALUE));
            nameValuePairs.add(new Pair<String, String>(CLIENT_VERSION, CLIENT_VERSION_VALUE));
            nameValuePairs.add(new Pair<String, String>(DEV_ID, DEV_ID_VALUE));
            nameValuePairs.add(new Pair<String, String>(FORMAT, WimConstants.FORMAT_JSON));
            nameValuePairs.add(new Pair<String, String>(ID_TYPE, "ICQ"));
            nameValuePairs.add(new Pair<String, String>(PASSWORD, icqAccountRoot.getUserPassword()));
            nameValuePairs.add(new Pair<String, String>(LOGIN, icqAccountRoot.getUserId()));

            try {
                // Execute request.
                InputStream responseStream = HttpUtil.executePost(loginConnection, HttpUtil.prepareParameters(nameValuePairs));
                String responseString = HttpUtil.streamToString(responseStream);
                responseStream.close();
                Log.d(Settings.LOG_TAG, "client login = " + responseString);

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
                        Log.d(Settings.LOG_TAG, "token a = " + tokenA);
                        Log.d(Settings.LOG_TAG, "sessionSecret = " + sessionSecret);
                        String sessionKey = StringUtil.getHmacSha256Base64(sessionSecret, icqAccountRoot.getUserPassword());
                        Log.d(Settings.LOG_TAG, "sessionKey = " + sessionKey);
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
            Log.d(Settings.LOG_TAG, "client login: " + e.getMessage());
            return INTERNAL_ERROR;
        }
    }

    public int startSession() {
        try {
            URL url = new URL(START_SESSION_URL);
            HttpURLConnection startSessionConnection = (HttpURLConnection) url.openConnection();
            startSessionConnection.setConnectTimeout(timeoutConnection);
            startSessionConnection.setReadTimeout(timeoutSocket);
            // Add your data
            List<Pair<String, String>> nameValuePairs = new ArrayList<Pair<String, String>>();
            nameValuePairs.add(new Pair<String, String>(WimConstants.TOKEN_A, icqAccountRoot.getTokenA()));
            nameValuePairs.add(new Pair<String, String>(ASSERT_CAPS, ASSERT_CAPS_VALUE));
            nameValuePairs.add(new Pair<String, String>(BUILD_NUMBER, BUILD_NUMBER_VALUE));
            nameValuePairs.add(new Pair<String, String>(CLIENT_NAME, CLIENT_NAME_VALUE));
            nameValuePairs.add(new Pair<String, String>(CLIENT_VERSION, CLIENT_VERSION_VALUE));
            nameValuePairs.add(new Pair<String, String>(DEVICE_ID, DEVICE_ID_VALUE));
            nameValuePairs.add(new Pair<String, String>(EVENTS, EVENTS_VALUE));
            nameValuePairs.add(new Pair<String, String>(FORMAT, WimConstants.FORMAT_JSON));
            nameValuePairs.add(new Pair<String, String>(IMF, "plain"));
            nameValuePairs.add(new Pair<String, String>(INCLUDE_PRESENCE_FIELDS, PRESENCE_FIELDS_VALUE));
            nameValuePairs.add(new Pair<String, String>(INVISIBLE, "false"));
            nameValuePairs.add(new Pair<String, String>(DEV_ID_K, DEV_ID_VALUE));
            nameValuePairs.add(new Pair<String, String>(LANGUAGE, "ru-ru"));
            nameValuePairs.add(new Pair<String, String>(MINIMIZE_RESPONSE, "0"));
            nameValuePairs.add(new Pair<String, String>(MOBILE, "1"));
            nameValuePairs.add(new Pair<String, String>(POLL_TIMEOUT, String.valueOf(timeoutConnection)));
            nameValuePairs.add(new Pair<String, String>(RAW_MSG, "0"));
            nameValuePairs.add(new Pair<String, String>(SESSION_TIMEOUT, String.valueOf(timeoutSession / 1000)));
            nameValuePairs.add(new Pair<String, String>(TS, String.valueOf(icqAccountRoot.getHostTime())));
            nameValuePairs.add(new Pair<String, String>(VIEW,
                    StatusUtil.getStatusValue(icqAccountRoot.getAccountType(), icqAccountRoot.getBaseStatusValue(icqAccountRoot.getStatusIndex()))));

            String hash = POST_PREFIX.concat(URLEncoder.encode(START_SESSION_URL, HttpUtil.UTF8_ENCODING))
                    .concat(AMP).concat(URLEncoder.encode(HttpUtil.prepareParameters(nameValuePairs), HttpUtil.UTF8_ENCODING));

            nameValuePairs.add(new Pair<String, String>("sig_sha256",
                    StringUtil.getHmacSha256Base64(hash, icqAccountRoot.getSessionKey())));
            Log.d(Settings.LOG_TAG, HttpUtil.prepareParameters(nameValuePairs));
            try {
                // Execute HTTP Post Request
                InputStream responseStream = HttpUtil.executePost(startSessionConnection, HttpUtil.prepareParameters(nameValuePairs));
                String responseString = HttpUtil.streamToString(responseStream);
                responseStream.close();
                Log.d(Settings.LOG_TAG, "start session = " + responseString);

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
                                dataObject.getJSONObject(MY_INFO).toString(), MyInfo.class);
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
                        if(moodStatusValue == SetMoodRequest.STATUS_MOOD_RESET) {
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
            Log.d(Settings.LOG_TAG, "start session exception", ex);
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
        Log.d(Settings.LOG_TAG, "start events fetching");
        do {
            try {
                URL url = new URL(getFetchUrl());
                HttpURLConnection fetchEventConnection = (HttpURLConnection) url.openConnection();
                fetchEventConnection.setConnectTimeout(timeoutConnection);
                fetchEventConnection.setReadTimeout(timeoutSocket);
                try {
                    InputStream responseStream = HttpUtil.executeGet(fetchEventConnection);
                    String responseString = HttpUtil.streamToString(responseStream);
                    responseStream.close();
                    Log.d(Settings.LOG_TAG, "fetch events = " + responseString);

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
                            Log.d(Settings.LOG_TAG, "Cycling all events.");
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
                            Log.d(Settings.LOG_TAG, "Something wend wrong. Let's reconnect if status is not offline.");
                            icqAccountRoot.resetLoginData();
                            icqAccountRoot.resetSessionData();
                            icqAccountRoot.updateAccount();
                            return icqAccountRoot.getStatusIndex() == StatusUtil.STATUS_OFFLINE;
                        }
                    }
                } finally {
                    fetchEventConnection.disconnect();
                }
            } catch (Throwable ex) {
                Log.d(Settings.LOG_TAG, "fetch events exception: " + ex.getMessage());
                try {
                    Thread.sleep(5000);
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
                .append(AMP).append(TIMEOUT).append(EQUAL).append(timeoutConnection)
                .append(AMP).append(R_PARAM).append(EQUAL).append(System.currentTimeMillis())
                .append(AMP).append(PEEK).append(EQUAL).append(0).toString();
    }

    private void processEvent(String eventType, JSONObject eventData) {
        Log.d(Settings.LOG_TAG, "eventType = " + eventType + "; eventData = " + eventData.toString());
        long processStartTime = System.currentTimeMillis();
        if (eventType.equals(BUDDYLIST)) {
            try {
                long updateTime = System.currentTimeMillis();
                int accountDbId = icqAccountRoot.getAccountDbId();
                String accountType = icqAccountRoot.getAccountType();
                ContentResolver contentResolver = icqAccountRoot.getContentResolver();

                JSONArray groupsArray = eventData.getJSONArray(GROUPS_ARRAY);

                for (int c = 0; c < groupsArray.length(); c++) {
                    JSONObject groupObject = groupsArray.getJSONObject(c);
                    String groupName = groupObject.getString(NAME);
                    int groupId = groupObject.getInt(ID_FIELD);
                    JSONArray buddiesArray = groupObject.getJSONArray(BUDDIES_ARRAY);

                    QueryHelper.updateOrCreateGroup(contentResolver, accountDbId, updateTime, groupName, groupId);

                    for (int i = 0; i < buddiesArray.length(); i++) {
                        JSONObject buddyObject = buddiesArray.getJSONObject(i);
                        String buddyId = buddyObject.getString(AIM_ID);
                        String buddyNick = buddyObject.optString(FRIENDLY);
                        if (TextUtils.isEmpty(buddyNick)) {
                            buddyNick = buddyObject.getString(DISPLAY_ID);
                        }

                        String buddyStatus = buddyObject.getString(STATE);
                        String moodIcon = buddyObject.optString(MOOD_ICON);
                        String statusMessage = buddyObject.optString(STATUS_MSG);
                        String moodTitle = buddyObject.optString(MOOD_TITLE);

                        int statusIndex = getStatusIndex(moodIcon, buddyStatus);
                        String statusTitle = getStatusTitle(moodTitle, statusIndex);

                        String buddyType = buddyObject.getString(USER_TYPE);
                        String buddyIcon = buddyObject.optString(BUDDY_ICON);

                        QueryHelper.updateOrCreateBuddy(contentResolver, accountDbId, accountType, updateTime,
                                groupId, groupName, buddyId, buddyNick, statusIndex, statusTitle, statusMessage,
                                buddyIcon);
                    }
                }
                QueryHelper.moveOutdatedBuddies(contentResolver, icqAccountRoot.getResources(), accountDbId, updateTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (eventType.equals(IM) || eventType.equals(OFFLINE_IM)) { // TODO: offlineIM is differ!
            try {
                String messageText = eventData.getString(MESSAGE);
                String cookie = eventData.optString(MSG_ID);
                if (TextUtils.isEmpty(cookie)) {
                    cookie = String.valueOf(System.currentTimeMillis());
                }
                long messageTime = eventData.getLong(TIMESTAMP);
                String imf = eventData.getString(IMF);
                String autoResponse = eventData.getString(AUTORESPONSE);
                JSONObject sourceObject = eventData.optJSONObject(SOURCE_OBJECT);
                String buddyId;
                String buddyNick;
                int statusIndex;
                String statusTitle;
                String statusMessage = "";
                String buddyIcon;
                if (sourceObject != null) {
                    buddyId = sourceObject.getString(AIM_ID);
                    buddyNick = sourceObject.optString(FRIENDLY);
                    String buddyStatus = sourceObject.getString(STATE);
                    String buddyType = sourceObject.getString(USER_TYPE);
                    buddyIcon = sourceObject.optString(BUDDY_ICON);
                    statusIndex = getStatusIndex(null, buddyStatus);
                } else {
                    buddyId = eventData.getString(AIM_ID);
                    buddyNick = eventData.optString(FRIENDLY);
                    buddyIcon = null;
                    statusIndex = StatusUtil.STATUS_OFFLINE;
                }
                if (TextUtils.isEmpty(buddyNick)) {
                    buddyNick = buddyId;
                }
                statusTitle = getStatusTitle(null, statusIndex);

                boolean isProcessed = false;
                do {
                    try {
                        QueryHelper.insertMessage(icqAccountRoot.getContentResolver(),
                                PreferenceHelper.isCollapseMessages(icqAccountRoot.getContext()),
                                icqAccountRoot.getAccountDbId(), buddyId, 1, 2, cookie, messageTime * 1000, messageText, true);
                        isProcessed = true;
                    } catch (BuddyNotFoundException ignored) {
                        String recycleString = icqAccountRoot.getResources().getString(R.string.recycle);
                        QueryHelper.updateOrCreateBuddy(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                                icqAccountRoot.getAccountType(), System.currentTimeMillis(), GlobalProvider.GROUP_ID_RECYCLE,
                                recycleString, buddyId, buddyNick, statusIndex, statusTitle, statusMessage, buddyIcon);
                    }
                    // This will try to create buddy if such is not present
                    // in roster and then retry message insertion.
                } while(!isProcessed);
            } catch (JSONException ex) {
                Log.d(Settings.LOG_TAG, "error while processing im - JSON exception", ex);
            }
        } else if (eventType.equals(IM_STATE)) {
            try {
                JSONArray imStatesArray = eventData.getJSONArray(IM_STATES_ARRAY);
                for (int c = 0; c < imStatesArray.length(); c++) {
                    JSONObject imState = imStatesArray.getJSONObject(c);
                    String state = imState.getString(STATE);
                    String msgId = imState.getString(MSG_ID);
                    String sendReqId = imState.optString(SEND_REQ_ID);
                    for (int i = 0; i < IM_STATES.length; i++) {
                        if (state.equals(IM_STATES[i])) {
                            QueryHelper.updateMessageState(icqAccountRoot.getContentResolver(), sendReqId, i);
                            break;
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.d(Settings.LOG_TAG, "error while processing im state", ex);
            }
        } else if (eventType.equals(PRESENCE)) {
            try {
                String buddyId = eventData.getString(AIM_ID);
                String buddyNick = eventData.optString(FRIENDLY);
                if (TextUtils.isEmpty(buddyNick)) {
                    buddyNick = eventData.getString(DISPLAY_ID);
                }

                String buddyStatus = eventData.getString(STATE);
                String moodIcon = eventData.optString(MOOD_ICON);
                String statusMessage = StringUtil.unescapeXml(eventData.optString(STATUS_MSG));
                String moodTitle = StringUtil.unescapeXml(eventData.optString(MOOD_TITLE));

                int statusIndex = getStatusIndex(moodIcon, buddyStatus);
                String statusTitle = getStatusTitle(moodTitle, statusIndex);

                String buddyType = eventData.getString(USER_TYPE);
                String buddyIcon = eventData.optString(BUDDY_ICON);

                QueryHelper.modifyBuddyStatus(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                        buddyId, statusIndex, statusTitle, statusMessage, buddyIcon);
            } catch (JSONException ex) {
                Log.d(Settings.LOG_TAG, "error while processing presence - JSON exception", ex);
            } catch (BuddyNotFoundException ex) {
                Log.d(Settings.LOG_TAG, "error while processing presence - buddy not found");
            }
        } else if (eventType.equals(MY_INFO)) {
            try {
                MyInfo myInfo = GsonSingleton.getInstance().fromJson(eventData.toString(), MyInfo.class);
                icqAccountRoot.setMyInfo(myInfo);
            } catch (Throwable ignored) {
                Log.d(Settings.LOG_TAG, "error while processing my info.");
            }
        } else if (eventType.equals(SESSION_ENDED)) {
            icqAccountRoot.resetLoginData();
            icqAccountRoot.resetSessionData();
            icqAccountRoot.carriedOff();
        }
        Log.d(Settings.LOG_TAG, "processed in " + (System.currentTimeMillis() - processStartTime) + " ms.");
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
        for (NameValuePair param : URLEncodedUtils.parse(uri, "UTF-8")) {
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
}