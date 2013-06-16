package com.tomclaw.mandarin.im.icq;

import android.util.Base64;
import android.util.Log;
import com.tomclaw.mandarin.core.Settings;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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

    public IcqSession(IcqAccountRoot icqAccountRoot) {
        this.icqAccountRoot = icqAccountRoot;
    }

    public void clientLogin() {

        /*try{
            String uriString = "http://api.icq.net/aim/startSession";
        String queryString = "a=%252FwQAAAAAAADOV4KGxJu%252BTnhTSVCYhphvEwU974OZlB%252F7%252FAlggNnJCQEqcy%252F4zDl%252FOuzwwpRYx9YjGNmDWdPj8b0XiYEc9nq8ZwXvy9JB4gJHOiWX3xzJQ2i30F%252F4ntl6S6xOKiSCZsyg1nsd%252FxZAiINswAnfHFfQkDHSo3yb0TwbiqC55z%252FH4W0xBHysLqEISi%252BGCQ%253D%253D&assertCaps=094613504C7F11D18222444553540000&buildNumber=1234&clientName=Android%20Agent&clientVersion=v0.01&deviceId=deviceid&events=myInfo%2Cpresence%2Cbuddylist%2Ctyping%2CimState%2Cim%2CsentIM%2CofflineIM%2CuserAddedToBuddyList%2Cservice%2CwebrtcMsg%2CbuddyRegistered&f=json&imf=plain&includePresenceFields=userType%2Cservice%2CmoodIcon%2CmoodTitle%2Ccapabilities%2CaimId%2CdisplayId%2Cfriendly%2Cstate%2CbuddyIcon%2CabPhones%2CsmsNumber%2CstatusMsg%2CseqNum%2CeventType&interestCaps=8eec67ce70d041009409a7c1602a5c84&invisible=false&k=ao1mAegmj4_7xQOy&language=ru-ru&minimizeResponse=0&mobile=1&pollTimeout=30000&rawMsg=0&sessionTimeout=1209600&ts=1371308179&view=mobile";
            String hash = "POST&" + URLEncoder.encode(uriString, "UTF-8") + "&" + URLEncoder.encode(queryString, "UTF-8");
        String sessionSecret = "gDJEf6VbogNwiKel";
        String sessionKey = getHmacSha256Base64(sessionSecret, "testacc1");
        String digest = getHmacSha256Base64(hash, sessionKey);
            Log.d(Settings.LOG_TAG, digest);
            // 2%2FhpWOCVTdwuICYysi%2BRqye7BeIlndBDwfjX%2BtP5Y2c%3D
        }catch(Throwable ex) {
            ex.printStackTrace();
        }
        if(true)return;*/

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

            Log.d(Settings.LOG_TAG, "response = " + responseString);

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

                startSession(tokenA, sessionKey, hostTime);
            }

        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "client login exception: " + e.getMessage());
        }
    }

    public void startSession(String tokenA, String sessionKey, long hostTime) {
        // Create a new HttpClient and Post Header
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://api.icq.net/aim/startSession");

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("a", tokenA));
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
            nameValuePairs.add(new BasicNameValuePair("ts", String.valueOf(hostTime)));
            nameValuePairs.add(new BasicNameValuePair("view", "mobile"));
            String hash = "POST&" + URLEncoder.encode("http://api.icq.net/aim/startSession", "UTF-8")
                    + "&" + URLEncoder.encode(EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)), "UTF-8");
            nameValuePairs.add(new BasicNameValuePair("sig_sha256", getHmacSha256Base64(hash, sessionKey)));

            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            Log.d(Settings.LOG_TAG, EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs)));

            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            Log.d(Settings.LOG_TAG, "start session = " + EntityUtils.toString(response.getEntity()));

        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "client login exception: " + e.getMessage());
        }
    }

    public String getHmacSha256Base64(String key, String data)
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
