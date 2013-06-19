package com.tomclaw.mandarin.im.icq;

import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.StatusUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
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

    private IcqAccountRoot icqAccountRoot;
    private Gson gson;

    public IcqSession(IcqAccountRoot icqAccountRoot) {
        this.icqAccountRoot = icqAccountRoot;
        this.gson = new Gson();
    }

    // TODO: more informative answer.
    public boolean clientLogin() {
        // Create a new HttpClient and Post Header
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(CLIENT_LOGIN_URL);
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(CLIENT_NAME, "Android%20Agent"));
            nameValuePairs.add(new BasicNameValuePair(CLIENT_VERSION, "3.2"));
            nameValuePairs.add(new BasicNameValuePair(DEV_ID, "ao1mAegmj4_7xQOy"));
            nameValuePairs.add(new BasicNameValuePair(FORMAT, "json"));
            nameValuePairs.add(new BasicNameValuePair(ID_TYPE, "ICQ"));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, "testacc1"));
            nameValuePairs.add(new BasicNameValuePair(LOGIN, "617401476"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "client login = " + responseString);
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            if (statusCode == 200) {
                JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                String login = dataObject.getString(LOGIN_ID);
                long hostTime = dataObject.getLong(HOST_TIME);
                String sessionSecret = dataObject.getString(SESSION_SECRET);
                JSONObject tokenObject = dataObject.getJSONObject(TOKEN_OBJECT);
                int expiresIn = tokenObject.getInt(EXPIRES_IN);
                String tokenA = tokenObject.getString(TOKEN_A);
                Log.d(Settings.LOG_TAG, "token a = " + tokenA);
                Log.d(Settings.LOG_TAG, "sessionSecret = " + sessionSecret);
                String sessionKey = getHmacSha256Base64(sessionSecret, "testacc1");
                Log.d(Settings.LOG_TAG, "sessionKey = " + sessionKey);
                // Update client login result in database.
                icqAccountRoot.setClientLoginResult(login, tokenA, sessionKey, expiresIn, hostTime);
                return true;
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "client login exception: " + e.getMessage());
        }
        return false;
    }

    // TODO: more informative answer.
    public boolean startSession() {
        // Create a new HttpClient and Post Header
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(START_SESSION_URL);
        try {
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
            nameValuePairs.add(new BasicNameValuePair(TIMESTAMP, String.valueOf(icqAccountRoot.getHostTime())));
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
            if (statusCode == 200) {
                JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                String aimSid = dataObject.getString(AIM_SID);
                String fetchBaseUrl = dataObject.getString(FETCH_BASE_URL);

                MyInfo myInfo = gson.fromJson(dataObject.getJSONObject(MY_INFO).toString(),
                        MyInfo.class);
                WellKnownUrls wellKnownUrls = gson.fromJson(
                        dataObject.getJSONObject(WELL_KNOWN_URLS).toString(), WellKnownUrls.class);

                // Update starts session result in database.
                icqAccountRoot.setStartSessionResult(aimSid, fetchBaseUrl, myInfo, wellKnownUrls);
                return true;
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "start session exception: " + e.getMessage());
        }
        return false;
    }

    public void endSession(String aimSid) {
        // Create a new HttpClient and Post Header
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(END_SESSION_URL);
        try {
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

    private String getHmacSha256Base64(String key, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String encryptionAlgorithm = "HmacSHA256";
        SecretKey secretKey = new SecretKeySpec(data.getBytes(), encryptionAlgorithm);
        Mac messageAuthenticationCode = Mac.getInstance(encryptionAlgorithm);
        messageAuthenticationCode.init(secretKey);
        messageAuthenticationCode.update(key.getBytes());
        byte[] digest = messageAuthenticationCode.doFinal();
        return Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    public void startEventsFetching() {
        Log.d(Settings.LOG_TAG, "start events fetching");
        // Create a new HttpClient and Post Header
        HttpClient httpClient = new DefaultHttpClient();
        do {
            HttpGet httpPost = new HttpGet(getFetchUrl());
            try {
                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                String responseString = EntityUtils.toString(response.getEntity());
                Log.d(Settings.LOG_TAG, "fetch events = " + responseString);
                JSONObject jsonObject = new JSONObject(responseString);
                JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
                int statusCode = responseObject.getInt(STATUS_CODE);
                if (statusCode == 200) {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    long hostTime = dataObject.getLong(TIMESTAMP);
                    String fetchBaseUrl = dataObject.getString(FETCH_BASE_URL);
                    // Update time and fetch base url.
                    icqAccountRoot.setHostTime(hostTime);
                    icqAccountRoot.setFetchBaseUrl(fetchBaseUrl);
                    // Store account state.
                    icqAccountRoot.updateAccount();
                    // Process events.
                    JSONArray eventsArray = dataObject.getJSONArray(EVENTS_ARRAY);
                    // Cycling all events.
                    for (int c = 0; c < eventsArray.length(); c++) {
                        JSONObject eventObject = eventsArray.getJSONObject(c);
                        String eventType = eventObject.getString(TYPE);
                        JSONObject eventData = eventObject.getJSONObject(EVENT_DATA_OBJECT);
                        // Process event.
                        processEvent(eventType, eventData);
                    }
                }
            } catch (Throwable e) {
                Log.d(Settings.LOG_TAG, "fetch events exception: " + e.getMessage());
            }
        } while (icqAccountRoot.getStatusIndex() != StatusUtil.STATUS_OFFLINE); // Fetching until online.
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
    }
}