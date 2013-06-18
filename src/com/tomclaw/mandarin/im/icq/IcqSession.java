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
        HttpPost httpPost = new HttpPost("https://api.login.icq.net/auth/clientLogin");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("clientName", "Android%20Agent"));
            nameValuePairs.add(new BasicNameValuePair("clientVersion", "3.2"));
            nameValuePairs.add(new BasicNameValuePair("devId", "ao1mAegmj4_7xQOy"));
            nameValuePairs.add(new BasicNameValuePair("f", "json"));
            nameValuePairs.add(new BasicNameValuePair("idType", "ICQ"));
            nameValuePairs.add(new BasicNameValuePair("pwd", "testacc1"));
            nameValuePairs.add(new BasicNameValuePair("s", "617401476"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "client login = " + responseString);
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject responseObject = jsonObject.getJSONObject("response");
            int statusCode = responseObject.getInt("statusCode");
            if (statusCode == 200) {
                JSONObject dataObject = responseObject.getJSONObject("data");
                String login = dataObject.getString("loginId");
                long hostTime = dataObject.getLong("hostTime");
                String sessionSecret = dataObject.getString("sessionSecret");
                JSONObject tokenObject = dataObject.getJSONObject("token");
                int expiresIn = tokenObject.getInt("expiresIn");
                String tokenA = tokenObject.getString("a");
                Log.d(Settings.LOG_TAG, "tokenA = " + tokenA);
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
        HttpPost httpPost = new HttpPost("http://api.icq.net/aim/startSession");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("a", icqAccountRoot.getTokenA()));
            nameValuePairs.add(new BasicNameValuePair("assertCaps", "094613504C7F11D18222444553540000"));
            nameValuePairs.add(new BasicNameValuePair("buildNumber", "1234"));
            nameValuePairs.add(new BasicNameValuePair("clientName", "Android%20Agent"));
            nameValuePairs.add(new BasicNameValuePair("clientVersion", "v0.01"));
            nameValuePairs.add(new BasicNameValuePair("deviceId", "deviceid"));
            nameValuePairs.add(new BasicNameValuePair("events", "myInfo,presence,buddylist,typing,imState,im,sentIM,offlineIM,userAddedToBuddyList,service,webrtcMsg,buddyRegistered"));
            nameValuePairs.add(new BasicNameValuePair("f", "json"));
            nameValuePairs.add(new BasicNameValuePair("imf", "plain"));
            nameValuePairs.add(new BasicNameValuePair("includePresenceFields", "userType,service,moodIcon,moodTitle,capabilities,aimId,displayId,friendly,state,buddyIcon,abPhones,smsNumber,statusMsg,seqNum,eventType"));
            nameValuePairs.add(new BasicNameValuePair("invisible", "false"));
            nameValuePairs.add(new BasicNameValuePair("k", "ao1mAegmj4_7xQOy"));
            nameValuePairs.add(new BasicNameValuePair("language", "ru-ru"));
            nameValuePairs.add(new BasicNameValuePair("minimizeResponse", "0"));
            nameValuePairs.add(new BasicNameValuePair("mobile", "1"));
            nameValuePairs.add(new BasicNameValuePair("pollTimeout", "30000"));
            nameValuePairs.add(new BasicNameValuePair("rawMsg", "0"));
            nameValuePairs.add(new BasicNameValuePair("sessionTimeout", "1209600"));
            nameValuePairs.add(new BasicNameValuePair("ts", String.valueOf(icqAccountRoot.getHostTime())));
            nameValuePairs.add(new BasicNameValuePair("view", "mobile"));
            String hash = "POST&" + URLEncoder.encode("http://api.icq.net/aim/startSession", "UTF-8")
                    + "&" + URLEncoder.encode(EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)), "UTF-8");
            nameValuePairs.add(new BasicNameValuePair("sig_sha256",
                    getHmacSha256Base64(hash, icqAccountRoot.getSessionKey())));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d(Settings.LOG_TAG, EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "start session = " + responseString);
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject responseObject = jsonObject.getJSONObject("response");
            int statusCode = responseObject.getInt("statusCode");
            if (statusCode == 200) {
                JSONObject dataObject = responseObject.getJSONObject("data");
                String aimSid = dataObject.getString("aimsid");
                String fetchBaseUrl = dataObject.getString("fetchBaseURL");

                MyInfo myInfo = gson.fromJson(dataObject.getJSONObject("myInfo").toString(),
                        MyInfo.class);
                WellKnownUrls wellKnownUrls = gson.fromJson(
                        dataObject.getJSONObject("wellKnownUrls").toString(), WellKnownUrls.class);

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
        HttpPost httpPost = new HttpPost("http://api.icq.net/aim/endSession");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("aimsid", aimSid));
            nameValuePairs.add(new BasicNameValuePair("f", "json"));
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
                JSONObject responseObject = jsonObject.getJSONObject("response");
                int statusCode = responseObject.getInt("statusCode");
                if (statusCode == 200) {
                    JSONObject dataObject = responseObject.getJSONObject("data");
                    long hostTime = dataObject.getLong("ts");
                    String fetchBaseUrl = dataObject.getString("fetchBaseURL");
                    // Update time and fetch base url.
                    icqAccountRoot.setHostTime(hostTime);
                    icqAccountRoot.setFetchBaseUrl(fetchBaseUrl);
                    // Store account state.
                    icqAccountRoot.updateAccount();
                    // Process events.
                    JSONArray eventsArray = dataObject.getJSONArray("events");
                    // Cycling all events.
                    for (int c = 0; c < eventsArray.length(); c++) {
                        JSONObject eventObject = eventsArray.getJSONObject(c);
                        String eventType = eventObject.getString("type");
                        JSONObject eventData = eventObject.getJSONObject("eventData");
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
        return icqAccountRoot.getFetchBaseUrl() + "&f=json&timeout=60000&r=" + System.currentTimeMillis() + "&peek=0";
    }

    private void processEvent(String eventType, JSONObject eventData) {
        Log.d(Settings.LOG_TAG, "eventType = " + eventType + "; eventData = " + eventData.toString());
    }
}