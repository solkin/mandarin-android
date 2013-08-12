package com.tomclaw.mandarin.im.vk;

import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.core.Settings;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 8/7/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class VKSession {

    public static final String redirect_url="http://oauth.vk.com/blank.html";
    public static final String BASE_URL="https://api.vk.com/method/";
    private static final String APP_ID = "3810777";
    private static String scopes = "friends,status,messages";

    public static final int LOGIN_SUCCESS = 0;
    public static final int LOGIN_ERROR = 1;

    private static final int timeoutConnection = 60000;
    private static final int timeoutSocket = 80000;

    private VkAccountRoot vkAccountRoot;
    private Gson gson;
    private HttpClient httpClient;

    public VKSession(VkAccountRoot vkAccountRoot){
         this.vkAccountRoot = vkAccountRoot;
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

    public static String getAuthUrl(){
        String url = "http://oauth.vk.com/authorize?client_id=" + APP_ID + "&scope=" + scopes +
                "&redirect_uri=" + redirect_url + "&display=mobile&v=5.0&response_type=token";
        return url;
    }

    /*Вызывает метод setOnline*/
    public int clientLogin() {
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair(VkConstants.ACCESS_TOKEN, vkAccountRoot.getToken()));

            HttpGet httpGet = new HttpGet(BASE_URL + VkConstants.SET_ONLINE + "?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
            HttpResponse response = httpClient.execute(httpGet);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "vk login response = " + responseString);

            /*
            если все хорошо, то {"response" : 1} , иначе
            {"error":{"error_code":5,"error_msg":"User authorization failed: no access_token passed.","request_params":[{"key":"oauth","value":"1"},{"key":"method","value":"account.setOnline"},{"key":"access_token","value":""}]}}
            */
            JSONObject jsonObject = new JSONObject(responseString);
            if (!jsonObject.has(VkConstants.RESPONSE_OBJECT)) {
                JSONObject errorObject = jsonObject.getJSONObject(VkConstants.ERROR);
                return LOGIN_ERROR;
            }
            return LOGIN_SUCCESS;
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "vk client login exception: " + e.getMessage());
            return LOGIN_ERROR;
        }
    }

    public boolean startEventsFetching() {
        try {
            Thread.sleep(50000);
        } catch (InterruptedException ignored) {
            // No need to check.
        }
        return true;
    }

    /*Вызывает метод setOffline*/
    public int disconnect() {
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair(VkConstants.ACCESS_TOKEN, vkAccountRoot.getToken()));
            HttpGet httpGet = new HttpGet(BASE_URL + VkConstants.SET_OFFLINE + "?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
            HttpResponse response = httpClient.execute(httpGet);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "vk disconnect response = " + responseString);

            JSONObject jsonObject = new JSONObject(responseString);
            if (!jsonObject.has(VkConstants.RESPONSE_OBJECT)) {
                JSONObject errorObject = jsonObject.getJSONObject(VkConstants.ERROR);
                return LOGIN_ERROR;
            }
            return LOGIN_SUCCESS;

        } catch (Exception e){
            return LOGIN_ERROR;
        }
    }
}
