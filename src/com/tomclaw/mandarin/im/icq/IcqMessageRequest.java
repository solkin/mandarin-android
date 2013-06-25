package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.Request;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
public class IcqMessageRequest extends WimRequest {

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
    public String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("im/sendIM");
    }

    @Override
    public List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("autoResponse", "false"));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("message", message));
        params.add(new Pair<String, String>("notifyDelivery", "true"));
        params.add(new Pair<String, String>("offlineIM", "true"));
        params.add(new Pair<String, String>("r", cookie));
        params.add(new Pair<String, String>("t", to));
        return params;
    }

    @Override
    public void onResponse() {
    }
}
