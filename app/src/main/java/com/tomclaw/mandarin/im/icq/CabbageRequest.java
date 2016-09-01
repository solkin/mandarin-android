package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.im.UrlEncodedBody;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ivsolkin on 30.08.16.
 */

public abstract class CabbageRequest extends Request<IcqAccountRoot> {

    private static final String CABBAGE_HOST = "https://rapi.icq.net";

    private static final OkHttpClient client = new OkHttpClient.Builder().build(); // TODO: replace with single instance

    private final String requestId;

    public CabbageRequest(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public int executeRequest() {
        Response response = null;
        try {
            RequestBody body = getBody();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(CABBAGE_HOST)
                    .method(HttpUtil.POST, body)
                    .build();
            response = client.newCall(request).execute();
            return parseResponse(response);
        } catch (Throwable ex) {
            Logger.log("Unable to execute request due to exception", ex);
            return REQUEST_PENDING;
        } finally {
            // Consume connection.
            if (response != null) {
                response.close();
            }
        }
    }

    protected abstract int parseResponse(Response response) throws Throwable;

    private RequestBody getBody() {
        JsonObject params = new JsonObject();
        appendParams(params);
        JsonObject root = new JsonObject();
        root.addProperty("method", getMethodName());
        root.addProperty("reqId", requestId);
        root.addProperty("authToken", getAccountRoot().getTokenCabbage());
        root.addProperty("icqAkes", WimConstants.DEV_ID);
        root.add("params", params);
        return new UrlEncodedBody(root.toString());
    }

    protected abstract String getMethodName();

    protected abstract void appendParams(JsonObject params);
}
