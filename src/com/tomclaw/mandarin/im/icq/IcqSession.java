package com.tomclaw.mandarin.im.icq;

import android.content.ContentResolver;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.StatusUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

    public static final int INTERNAL_ERROR = 1000;
    public static final int EXTERNAL_LOGIN_OK = 200;
    public static final int EXTERNAL_LOGIN_ERROR = 330;
    public static final int EXTERNAL_UNKNOWN = 0;
    private static final int EXTERNAL_SESSION_OK = 200;
    private static final int EXTERNAL_FETCH_OK = 200;

    private static final int timeoutConnection = 60000;
    private static final int timeoutSocket = 80000;

    private IcqAccountRoot icqAccountRoot;
    private Gson gson;
    // Connection and fetch events client.
    private HttpClient httpClient;

    public IcqSession(IcqAccountRoot icqAccountRoot) {
        this.icqAccountRoot = icqAccountRoot;
        this.gson = new Gson();
        // Creating Http params.
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT).
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        this.httpClient = new DefaultHttpClient(httpParameters);
    }

    public int clientLogin() {
        try {
            // Create a new post header.
            HttpPost httpPost = new HttpPost(CLIENT_LOGIN_URL);
            // Specifying login data.
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(CLIENT_NAME, "Android%20Agent"));
            nameValuePairs.add(new BasicNameValuePair(CLIENT_VERSION, "3.2"));
            nameValuePairs.add(new BasicNameValuePair(DEV_ID, "ao1mAegmj4_7xQOy"));
            nameValuePairs.add(new BasicNameValuePair(FORMAT, "json"));
            nameValuePairs.add(new BasicNameValuePair(ID_TYPE, "ICQ"));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, icqAccountRoot.getUserPassword()));
            nameValuePairs.add(new BasicNameValuePair(LOGIN, icqAccountRoot.getUserId()));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute request.
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
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
                    String sessionKey = getHmacSha256Base64(sessionSecret, icqAccountRoot.getUserPassword());
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
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "client login exception: " + e.getMessage());
            return INTERNAL_ERROR;
        }
    }

    public int startSession() {
        try {
            // Create a new HttpClient and Post Header
            HttpPost httpPost = new HttpPost(START_SESSION_URL);
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(WimConstants.TOKEN_A, icqAccountRoot.getTokenA()));
            nameValuePairs.add(new BasicNameValuePair(ASSERT_CAPS, "094613504C7F11D18222444553540000"));
            nameValuePairs.add(new BasicNameValuePair(BUILD_NUMBER, "1234"));
            nameValuePairs.add(new BasicNameValuePair(CLIENT_NAME, "Android%20Agent"));
            nameValuePairs.add(new BasicNameValuePair(CLIENT_VERSION, "v0.01"));
            nameValuePairs.add(new BasicNameValuePair(DEVICE_ID, "deviceid"));
            nameValuePairs.add(new BasicNameValuePair(EVENTS, "myInfo,presence,buddylist,typing,imState,im,sentIM,offlineIM,userAddedToBuddyList,service,webrtcMsg,buddyRegistered"));
            nameValuePairs.add(new BasicNameValuePair(FORMAT, "json"));
            nameValuePairs.add(new BasicNameValuePair(IMF, "plain"));
            nameValuePairs.add(new BasicNameValuePair(INCLUDE_PRESENCE_FIELDS, "userType,service,moodIcon,moodTitle,capabilities,aimId,displayId,friendly,state,buddyIcon,abPhones,smsNumber,statusMsg,seqNum,eventType"));
            nameValuePairs.add(new BasicNameValuePair(INVISIBLE, "false"));
            nameValuePairs.add(new BasicNameValuePair(DEV_ID_K, "ao1mAegmj4_7xQOy"));
            nameValuePairs.add(new BasicNameValuePair(LANGUAGE, "ru-ru"));
            nameValuePairs.add(new BasicNameValuePair(MINIMIZE_RESPONSE, "0"));
            nameValuePairs.add(new BasicNameValuePair(MOBILE, "1"));
            nameValuePairs.add(new BasicNameValuePair(POLL_TIMEOUT, "30000"));
            nameValuePairs.add(new BasicNameValuePair(RAW_MSG, "0"));
            nameValuePairs.add(new BasicNameValuePair(SESSION_TIMEOUT, "1209600"));
            nameValuePairs.add(new BasicNameValuePair(TS, String.valueOf(icqAccountRoot.getHostTime())));
            nameValuePairs.add(new BasicNameValuePair(VIEW, "mobile"));
            String hash = POST_PREFIX.concat(URLEncoder.encode(START_SESSION_URL, "UTF-8"))
                    .concat(AMP).concat(URLEncoder.encode(EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)), "UTF-8"));
            nameValuePairs.add(new BasicNameValuePair("sig_sha256",
                    getHmacSha256Base64(hash, icqAccountRoot.getSessionKey())));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d(Settings.LOG_TAG, EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "start session = " + responseString);
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            switch (statusCode) {
                case EXTERNAL_LOGIN_OK: {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    String aimSid = dataObject.getString(AIM_SID);
                    String fetchBaseUrl = dataObject.getString(FETCH_BASE_URL);

                    MyInfo myInfo = gson.fromJson(dataObject.getJSONObject(MY_INFO).toString(),
                            MyInfo.class);
                    WellKnownUrls wellKnownUrls = gson.fromJson(
                            dataObject.getJSONObject(WELL_KNOWN_URLS).toString(), WellKnownUrls.class);

                    // Update starts session result in database.
                    icqAccountRoot.setStartSessionResult(aimSid, fetchBaseUrl, myInfo, wellKnownUrls);
                    return EXTERNAL_SESSION_OK;
                }
                // TODO: may be cases if ts incorrect. Mey be proceed too.
                default: {
                    return EXTERNAL_UNKNOWN;
                }
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "start session exception: " + e.getMessage());
            return INTERNAL_ERROR;
        }
    }

    public void endSession(String aimSid) {
        try {
            // Create a new HttpClient and Post Header
            HttpPost httpPost = new HttpPost(END_SESSION_URL);
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(AIM_SID, aimSid));
            nameValuePairs.add(new BasicNameValuePair(FORMAT, "json"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            Log.d(Settings.LOG_TAG, "end session = " + EntityUtils.toString(response.getEntity()));
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "end session exception: " + e.getMessage());
        }
    }

    /**
     * Start event fetching in verbal cycle.
     *
     * @return true if we are now in offline mode because of user decision.
     *         false if our session is not accepted by the server.
     */
    public boolean startEventsFetching() {
        Log.d(Settings.LOG_TAG, "start events fetching");
        do {
            try {
                HttpGet httpGet = new HttpGet(getFetchUrl());
                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpGet);
                String responseString = EntityUtils.toString(response.getEntity());
                Log.d(Settings.LOG_TAG, "fetch events = " + responseString);
                JSONObject jsonObject = new JSONObject(responseString);
                JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
                int statusCode = responseObject.getInt(STATUS_CODE);
                switch (statusCode) {
                    case EXTERNAL_FETCH_OK: {
                        JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                        long hostTime = dataObject.getLong(TS);
                        String fetchBaseUrl = dataObject.getString(FETCH_BASE_URL);
                        // Update time and fetch base url.
                        icqAccountRoot.setHostTime(hostTime);
                        icqAccountRoot.setFetchBaseUrl(fetchBaseUrl);
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
                        // Something wend wrong. Let's reconnect.
                        // Reset login and session data.
                        icqAccountRoot.resetLoginData();
                        icqAccountRoot.resetSessionData();
                        icqAccountRoot.updateAccount();
                        return false;
                    }
                }
            } catch (Throwable e) {
                Log.d(Settings.LOG_TAG, "fetch events exception: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    // We'll sleep while there is no network connection.
                }
            }
        } while (icqAccountRoot.getStatusIndex() != StatusUtil.STATUS_OFFLINE); // Fetching until online.
        return true;
    }

    public String getFetchUrl() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(icqAccountRoot.getFetchBaseUrl());
        stringBuilder.append(AMP).append(FORMAT).append(EQUAL).append("json");
        stringBuilder.append(AMP).append(TIMEOUT).append(EQUAL).append(60000);
        stringBuilder.append(AMP).append(R_PARAM).append(EQUAL).append(System.currentTimeMillis());
        stringBuilder.append(AMP).append(PEEK).append(EQUAL).append(0);
        return stringBuilder.toString();
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
                        String buddyType = buddyObject.getString(USER_TYPE);
                        String buddyIcon = buddyObject.optString(BUDDY_ICON);

                        QueryHelper.updateOrCreateBuddy(contentResolver, accountDbId, accountType, updateTime,
                                groupName, buddyId, buddyNick, buddyStatus);
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
                JSONObject sourceObject = eventData.getJSONObject(SOURCE_OBJECT);
                String buddyId = sourceObject.getString(AIM_ID);
                String buddyNick = sourceObject.optString(FRIENDLY);
                if (TextUtils.isEmpty(buddyNick)) {
                    buddyNick = sourceObject.getString(DISPLAY_ID);
                }
                String buddyStatus = sourceObject.getString(STATE);
                String buddyType = sourceObject.getString(USER_TYPE);
                
                QueryHelper.insertMessage(icqAccountRoot.getContentResolver(), CoreService.getAppSession(),
                        icqAccountRoot.getAccountDbId(), buddyId, 1, cookie, messageTime * 1000, messageText, true);
                icqAccountRoot.getNotificationHelper().notifyAboutMessage(icqAccountRoot.getAccountDbId(), buddyId, messageText);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (BuddyNotFoundException e) {
                e.printStackTrace();
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
                            QueryHelper.updateMessage(icqAccountRoot.getContentResolver(), sendReqId, i);
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (eventType.equals(PRESENCE)) {
            try {
                String buddyId = eventData.getString(AIM_ID);
                String buddyNick = eventData.optString(FRIENDLY);
                if (TextUtils.isEmpty(buddyNick)) {
                    buddyNick = eventData.getString(DISPLAY_ID);
                }
                String buddyStatus = eventData.getString(STATE);
                String buddyStatusMessage = eventData.optString(STATUS_MSG);
                String buddyType = eventData.getString(USER_TYPE);

                QueryHelper.modifyBuddyStatus(icqAccountRoot.getContentResolver(), icqAccountRoot.getAccountDbId(),
                        buddyId, IcqStatusUtil.getStatusIndex(buddyStatus));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (BuddyNotFoundException e) {
                e.printStackTrace();
            }
        }
        Log.d(Settings.LOG_TAG, "processed in " + (System.currentTimeMillis() - processStartTime) + " ms.");
    }

    private static String getHmacSha256Base64(String key, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String encryptionAlgorithm = "HmacSHA256";
        SecretKey secretKey = new SecretKeySpec(data.getBytes(), encryptionAlgorithm);
        Mac messageAuthenticationCode = Mac.getInstance(encryptionAlgorithm);
        messageAuthenticationCode.init(secretKey);
        messageAuthenticationCode.update(key.getBytes());
        byte[] digest = messageAuthenticationCode.doFinal();
        return Base64.encodeToString(digest, Base64.NO_WRAP);
    }
}