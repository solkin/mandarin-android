package com.tomclaw.mandarin.core;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.WimConstants;
import com.tomclaw.mandarin.util.HttpUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.UnsupportedEncodingException;
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
            HttpClient httpClient = getHttpClient();
            HttpResponse response = httpClient.execute(getHttpRequestBase(getUrlWithParameters()));
            // Process response.
            int result = parseResponse(response);
            try {
                // Release connection.
                response.getEntity().consumeContent();
            } catch (Throwable ignored) {
                Log.d(Settings.LOG_TAG, "Unable to consume content in http request.");
            }
            return result;
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "Unable to execute request due to exception", e);
            return REQUEST_PENDING;
        }
    }

    public abstract HttpClient getHttpClient();

    /**
     * Returns request-specific request base: HttpGet or HttpPost.
     * @param url
     * @return HttpRequestBase, that will be executed.
     */
    protected abstract HttpRequestBase getHttpRequestBase(String url);

    /**
     * This method parses HttpResponse from server and returns request status.
     * @param httpResponse
     * @return int - request status.
     * @throws Throwable
     */
    protected abstract int parseResponse(HttpResponse httpResponse) throws Throwable;

    /**
     * Returns request-specific base Url (most of all from WellKnownUrls).
     * @return Request-specific base Url.
     */
    protected abstract String getUrl();

    /**
     * Returns parameters, must be appended to the Get request.
     * @return List of Get parameters.
     */
    protected abstract List<Pair<String, String>> getParams();

    /**
     * Returns url with prepared parameters to perform Get request.
     * @return String - prepared Url.
     * @throws UnsupportedEncodingException
     */
    private String getUrlWithParameters() throws UnsupportedEncodingException {
        // Obtain request-specific url.
        String url = getUrl();
        String parameters = HttpUtil.prepareParameters(getParams());
        Log.d(Settings.LOG_TAG, "try to send request to ".concat(url).concat(" with parameters: ")
                .concat(WimConstants.QUE).concat(parameters));
        if(!TextUtils.isEmpty(parameters)) {
            url = url.concat(WimConstants.QUE).concat(parameters);
        }
        return url;
    }
}
