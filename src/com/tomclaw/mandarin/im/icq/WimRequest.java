package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.HttpRequest;
import com.tomclaw.mandarin.core.Settings;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/23/13
 * Time: 9:24 PM
 */
public abstract class WimRequest extends HttpRequest<IcqAccountRoot> {

    protected static final transient int WIM_OK = 200;
    protected static final transient int WIM_AUTH_REQUIRED = 401;

    @Override
    protected HttpRequestBase getHttpRequestBase(String url) {
        return new HttpGet(url);
    }

    @Override
    protected final int parseResponse(HttpResponse httpResponse) throws Throwable {
        String responseString = EntityUtils.toString(httpResponse.getEntity());
        Log.d(Settings.LOG_TAG, "sent request = ".concat(responseString));
        return parseJson(new JSONObject(responseString));
    }

    protected abstract int parseJson(JSONObject response) throws JSONException;
}
