package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.HttpRequest;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

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
    protected String getHttpRequestType() {
        return HttpUtil.GET;
    }

    @Override
    protected final int parseResponse(InputStream httpResponseStream) throws Throwable {
        String responseString = HttpUtil.streamToString(httpResponseStream);
        Log.d(Settings.LOG_TAG, "sent request = ".concat(responseString));
        return parseJson(parseResponse(responseString));
    }

    protected JSONObject parseResponse(String responseString) throws JSONException {
        return new JSONObject(responseString);
    }

    protected abstract int parseJson(JSONObject response) throws JSONException;
}
