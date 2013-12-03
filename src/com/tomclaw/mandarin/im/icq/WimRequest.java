package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.util.HttpUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/23/13
 * Time: 9:24 PM
 */
public abstract class WimRequest extends Request<IcqAccountRoot> {

    protected static final transient int WIM_OK = 200;
    protected static final transient int WIM_AUTH_REQUIRED = 401;

    // One http client for all Wim requests, cause they invokes coherently.
    private static final transient HttpClient httpClient;

    static {
        httpClient = new DefaultHttpClient();
    }

    @Override
    public int buildRequest() {
        try {
            // Obtain request-specific url.
            String url = getUrl();
            String parameters = HttpUtil.prepareParameters(getParams());
            Log.d(Settings.LOG_TAG, "try to send request to ".concat(url)
                    .concat(" with parameters: ").concat(WimConstants.QUE).concat(parameters));

            HttpGet httpGet = new HttpGet(url.concat(WimConstants.QUE).concat(parameters));
            HttpResponse response = httpClient.execute(httpGet);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "sent request = ".concat(responseString));

            return parseResponse(new JSONObject(responseString));
        } catch (Throwable e) {
            e.printStackTrace();
            return REQUEST_PENDING;
        }
    }

    public abstract int parseResponse(JSONObject response) throws JSONException;

    /**
     * Returns request-specific base Url (most of all from WellKnownUrls).
     * @return Request-specific base Url.
     */
    public abstract String getUrl();

    /**
     * Returns parameters, must be appended to the Get request.
     * @return List of Get parameters.
     */
    public abstract List<Pair<String, String>> getParams();
}
