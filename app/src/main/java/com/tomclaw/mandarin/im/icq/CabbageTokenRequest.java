package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.im.UrlEncodedBody;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import okio.Buffer;

/**
 * Created by ivsolkin on 28.09.16.
 */

public class CabbageTokenRequest extends WimRequest {

    public CabbageTokenRequest() {
    }

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.POST;
    }
    @Override
    protected byte[] getBody() throws Throwable {
        Buffer buffer = new Buffer();
        try {
            HttpParamsBuilder paramsBuilder = getParams();
            String signed = getAccountRoot().getSession().signRequest(
                    getHttpRequestType(), getUrl(), false, paramsBuilder);
            final String query = new URL(signed).getQuery();
            UrlEncodedBody urlEncodedBody = new UrlEncodedBody(query);
            urlEncodedBody.writeTo(buffer);
            return buffer.readByteArray();
        } finally {
            buffer.clear();
        }
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        long timeStamp = response.getLong("ts");
        getAccountRoot().setHostTime(timeStamp);
        JSONObject statusObject = response.getJSONObject("status");
        int statusCode = statusObject.getInt("code");
        if (CabbageRequest.isSuccess(statusCode)) {
            JSONObject resultsObject = response.getJSONObject("results");
            return parseResults(resultsObject);
        } else if (CabbageRequest.isClientExpired(statusCode)) {
            getAccountRoot().onCabbageClientExpired();
            return REQUEST_PENDING;
        } else if (CabbageRequest.isTokenExpired(statusCode)) {
            getAccountRoot().onCabbageTokenExpired();
            return REQUEST_PENDING;
        } else {
            return REQUEST_DELETE;
        }
    }

    private int parseResults(JSONObject resultsObject) throws JSONException {
        String authToken = resultsObject.getString("authToken");
        getAccountRoot().onCabbageTokenObtained(authToken);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return "https://rapi.icq.net/genToken";
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder();
    }
}
