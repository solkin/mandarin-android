package com.tomclaw.mandarin.core;

import android.text.TextUtils;

import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.WimConstants;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.ParamsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/5/13
 * Time: 2:09 PM
 */
public abstract class HttpRequest<A extends AccountRoot> extends Request<A> {

    @Override
    public int executeRequest() {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            boolean isGetRequest = getHttpRequestType().equals(HttpUtil.GET);
            url = new URL(isUrlWithParameters() ? getUrlWithParameters() : getUrl());
            urlConnection = (HttpURLConnection) url.openConnection();
            Map<String, String> props = getRequestProperties();
            for (String key : props.keySet()) {
                String value = props.get(key);
                urlConnection.setRequestProperty(key, value);
            }
            // Executing request.
            InputStream in = isGetRequest ?
                    HttpUtil.executeGet(urlConnection) :
                    HttpUtil.executePost(urlConnection, getBody());
            int result = parseResponse(in);
            // Almost done. Close stream.
            in.close();
            return result;
        } catch (Throwable e) {
            Logger.log("Unable to execute request due to exception", e);
            return REQUEST_PENDING;
        } finally {
            // Trying to disconnect in any case.
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
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
     * @param httpResponseStream - stream to be parsed.
     * @return int - request status.
     * @throws Throwable
     */
    protected abstract int parseResponse(InputStream httpResponseStream) throws Throwable;

    protected byte[] getBody() throws IOException {
        return HttpUtil.stringToArray(getParams().build());
    }

    /**
     * Returns request-specific base Url (most of all from WellKnownUrls).
     *
     * @return Request-specific base Url.
     */
    protected abstract String getUrl();

    protected boolean isUrlWithParameters() {
        return getHttpRequestType().equals(HttpUtil.GET);
    }

    /**
     * Returns parameters, must be appended to the Get request.
     *
     * @return List of Get parameters.
     */
    protected abstract ParamsBuilder getParams();

    /**
     * Returns url with prepared parameters to perform Get request.
     *
     * @return String - prepared Url.
     * @throws UnsupportedEncodingException
     */
    private String getUrlWithParameters() throws UnsupportedEncodingException {
        // Obtain request-specific url.
        String url = getUrl();
        String parameters = getParams().build();
        Logger.log("try to send request to ".concat(url).concat(" with parameters: ")
                .concat(WimConstants.QUE).concat(parameters));
        if (!TextUtils.isEmpty(parameters)) {
            url = url.concat(WimConstants.QUE).concat(parameters);
        }
        return url;
    }

    public Map<String, String> getRequestProperties() {
        return Collections.emptyMap();
    }
}
