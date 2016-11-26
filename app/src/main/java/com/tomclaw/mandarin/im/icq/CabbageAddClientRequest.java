package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONObject;

/**
 * Created by ivsolkin on 28.09.16.
 */
public class CabbageAddClientRequest extends CabbageRequest {

    private final String userAgent;
    private final int buildNumber;
    private final String versionName;

    public CabbageAddClientRequest(String userAgent, int buildNumber, String versionName) {
        super();
        this.userAgent = userAgent;
        this.buildNumber = buildNumber;
        this.versionName = versionName;
    }

    @Override
    protected int parseResults(JSONObject results) throws Throwable {
        long clientId = results.getLong("clientId");
        String appStamp = results.getString("appStamp");
        Logger.log("cabbage client added: " + clientId + " (appStamp: " + appStamp + ")");
        getAccountRoot().onCabbageClientObtained(clientId, appStamp);
        return REQUEST_DELETE;
    }

    @Override
    protected String getMethodName() {
        return "addClient";
    }

    @Override
    protected void addProperty(JsonObject root) {
    }

    @Override
    protected void appendParams(JsonObject params) {
        JsonObject uaObject = new JsonObject();
        uaObject.addProperty("os", "android");
        uaObject.addProperty("app", "icq");
        uaObject.addProperty("label", userAgent);
        uaObject.addProperty("build", buildNumber);
        uaObject.addProperty("version", versionName);
        params.add("ua", uaObject);
    }
}
