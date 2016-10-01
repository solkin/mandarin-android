package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.im.UrlEncodedBody;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONObject;

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
    private String authToken;

    public CabbageRequest(String requestId, String authToken) {
        this.requestId = requestId;
        this.authToken = authToken;
    }

    @Override
    public int executeRequest() {
        Response response = null;
        try {
            RequestBody body = getBody();
            Logger.logRequest("cabbage", body);
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

    private int parseResponse(Response response) throws Throwable {
        String responseBody = response.body().string();
        Logger.logResponse("cabbage", responseBody);
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONObject statusObject = jsonResponse.getJSONObject("status");
        int statusCode = statusObject.getInt("code");
        if (isSuccess(statusCode)) {
            JSONObject resultsObject = jsonResponse.optJSONObject("results");
            if (resultsObject != null) {
                return parseResults(resultsObject);
            } else {
                return REQUEST_DELETE;
            }
        } else if (isClientExpired(statusCode)) {
            getAccountRoot().onCabbageClientExpired();
            return REQUEST_SKIP;
        } else if (isTokenExpired(statusCode)) {
            getAccountRoot().onCabbageTokenExpired();
            return REQUEST_SKIP;
        } else {
            return REQUEST_PENDING;
        }
    }

    protected abstract int parseResults(JSONObject results) throws Throwable;

    private RequestBody getBody() {
        JsonObject root = new JsonObject();
        root.addProperty("method", getMethodName());
        root.addProperty("reqId", requestId);
        root.addProperty("authToken", authToken);
        addProperty(root);
        JsonObject params = new JsonObject();
        appendParams(params);
        root.add("params", params);
        return new UrlEncodedBody(root.toString());
    }

    protected abstract void addProperty(JsonObject root);

    protected abstract String getMethodName();

    protected abstract void appendParams(JsonObject params);

    public static boolean isSuccess(int code) {
        return code / 100 == 200;
    }

    public static boolean isTokenExpired(int code) {
        return code / 100 == 402;
    }

    public static boolean isClientExpired(int code) {
        return code / 100 == 403;
    }
}
