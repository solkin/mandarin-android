package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.mandarin.core.HttpRequest;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

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

    protected static final int WIM_OK = 200;
    protected static final int CABBAGE_OK = 20000;
    protected static final int WIM_AUTH_REQUIRED = 401;

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.GET;
    }

    @Override
    protected final int parseResponse(InputStream httpResponseStream) throws Throwable {
        String responseString = HttpUtil.streamToString(httpResponseStream);
        Logger.log("sent request = ".concat(responseString));
        return parseJson(parseResponse(responseString));
    }

    protected JSONObject parseResponse(String responseString) throws JSONException {
        if (TextUtils.isEmpty(responseString)) {
            return new JSONObject();
        }
        return new JSONObject(responseString);
    }

    protected abstract int parseJson(JSONObject response) throws JSONException;
}
