package com.tomclaw.mandarin.core;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.WimConstants;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/5/13
 * Time: 2:09 PM
 */
public abstract class HttpRequest<A extends AccountRoot> extends Request<A> {

    @Override
    public int executeRequest() {
        try {
            if (getHttpRequestType().equals("GET")) {
                URL url = new URL(getUrlWithParameters());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(getHttpRequestType());

                try {
                    InputStream in = urlConnection.getInputStream();
                    int result = parseResponse(in);
                    in.close();
                    return result;
                } catch (Throwable e) {
                    Log.d(Settings.LOG_TAG, "Unable to execute request due to exception", e);
                    return REQUEST_PENDING;
                } finally {
                    urlConnection.disconnect();
                }
            } else {
                // Here will be POST
                return 0;
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "Unable to execute request due to exception", e);
            return REQUEST_PENDING;
        }
    }

    /**
     * Returns HTTP request method: GET or POST.
     *
     * @return request method.
     */
    protected abstract String getHttpRequestType();

    /**
     * This method parses String response from server and returns request status.
     *
     * @param httpResponseStream
     * @return int - request status.
     * @throws Throwable
     */
    protected abstract int parseResponse(InputStream httpResponseStream) throws Throwable;

    /**
     * Returns request-specific base Url (most of all from WellKnownUrls).
     *
     * @return Request-specific base Url.
     */
    protected abstract String getUrl();

    /**
     * Returns parameters, must be appended to the Get request.
     *
     * @return List of Get parameters.
     */
    protected abstract List<Pair<String, String>> getParams();

    /**
     * Returns url with prepared parameters to perform Get request.
     *
     * @return String - prepared Url.
     * @throws UnsupportedEncodingException
     */
    private String getUrlWithParameters() throws UnsupportedEncodingException {
        // Obtain request-specific url.
        String url = getUrl();
        String parameters = HttpUtil.prepareParameters(getParams());
        Log.d(Settings.LOG_TAG, "try to send request to ".concat(url).concat(" with parameters: ")
                .concat(WimConstants.QUE).concat(parameters));
        if (!TextUtils.isEmpty(parameters)) {
            url = url.concat(WimConstants.QUE).concat(parameters);
        }
        return url;
    }
}
