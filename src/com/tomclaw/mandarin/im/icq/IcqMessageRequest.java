package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.Request;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/12/13
 * Time: 1:38 PM
 */
public class IcqMessageRequest extends Request {

    private String to;
    private String message;
    private String cookie;

    public IcqMessageRequest() {
    }

    public IcqMessageRequest(String to, String message, String cookie) {
        this.to = to;
        this.message = message;
        this.cookie = cookie;
    }

    @Override
    public int onRequest(AccountRoot accountRoot) {
        // Create a new HttpClient and Post Header
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("aimsid", ((IcqAccountRoot)accountRoot).getAimSid()));
            nameValuePairs.add(new BasicNameValuePair("autoResponse", "false"));
            nameValuePairs.add(new BasicNameValuePair("f", "json"));
            nameValuePairs.add(new BasicNameValuePair("message", message));
            nameValuePairs.add(new BasicNameValuePair("notifyDelivery", "true"));
            nameValuePairs.add(new BasicNameValuePair("offlineIM", "true"));
            nameValuePairs.add(new BasicNameValuePair("r", cookie));
            nameValuePairs.add(new BasicNameValuePair("t", to));

            String url = "http://api.icq.net/im/sendIM?aimsid="+encode(((IcqAccountRoot)accountRoot).getAimSid())
                    +"&autoResponse=false&f=json&message="+encode(message)+"&notifyDelivery=true&offlineIM=true&r="
                    +encode(cookie)+"&t="+encode(to);
            Log.d(Settings.LOG_TAG, url);
            // Execute HTTP Post Request
            HttpGet httpPost = new HttpGet(url);
            HttpResponse response = ((IcqAccountRoot)accountRoot).getSession().getHttpClient().execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "send im = " + responseString);
        } catch (Throwable e) {
            e.printStackTrace();
            return REQUEST_PENDING;
        }
        return REQUEST_DELETE;
    }

    public static String encode(final String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onResponse() {

    }
}
