package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

import org.json.JSONObject;

/**
 * Created by ivsolkin on 28.09.16.
 */
public class CabbageModClientRequest extends CabbageTrueRequest {

    private final String userAgent;
    private final int buildNumber;
    private final String versionName;

    public CabbageModClientRequest(String userAgent, int buildNumber, String versionName) {
        super();
        this.userAgent = userAgent;
        this.buildNumber = buildNumber;
        this.versionName = versionName;
    }

    @Override
    protected int parseResults(JSONObject results) throws Throwable {
        long clientId = results.getLong("clientId");
        String appStamp = results.getString("appStamp");
        getAccountRoot().onCabbageClientObtained(clientId, appStamp);
        return REQUEST_DELETE;
    }

    @Override
    protected String getMethodName() {
        return "modClient";
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
