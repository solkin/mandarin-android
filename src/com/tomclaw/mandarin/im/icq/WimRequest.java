package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Request;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/23/13
 * Time: 9:24 PM
 */
public abstract class WimRequest extends Request<IcqAccountRoot> {

    protected static final transient int WIM_OK = 200;

    // One ttp client for all Wim requests, cause they invokes coherently.
    private static final transient HttpClient httpClient;

    static {
        httpClient = new DefaultHttpClient();
    }

    @Override
    public int buildRequest() {
        try {
            // Obtain request-specific url.
            String url = getUrl();
            String parameters = prepareParameters(getParams());
            Log.d(Settings.LOG_TAG, "try to send request to ".concat(url)
                    .concat(" with parameters: ").concat(parameters));

            HttpGet httpGet = new HttpGet(url.concat(parameters));
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

    /**
     * Builds Url request string from specified parameters.
     * @param pairs
     * @return String - Url request parameters.
     * @throws UnsupportedEncodingException
     */
    public static String prepareParameters(List<Pair<String, String>> pairs)
            throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        // Perform pair concatenation.
        for (Pair<String, String> pair : pairs) {
            if (builder.length() == 0) {
                builder.append(WimConstants.QUE);
            } else {
                builder.append(WimConstants.AMP);
            }
            builder.append(pair.first)
                    .append(WimConstants.EQUAL)
                    .append(URLEncoder.encode(pair.second, "UTF-8")
                            .replace("+", "%20"));
        }
        return builder.toString();
    }
}
