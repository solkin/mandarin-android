package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

import org.json.JSONObject;

/**
 * Created by ivsolkin on 26.11.16.
 */
public class HistoryBlockRequest extends CabbageTrueRequest {

    private final String buddyId;
    private final long fromMessageId;
    private final long tillMessageId;
    private final String patchVersion;
    private final int count;

    public HistoryBlockRequest(String buddyId, long fromMessageId, long tillMessageId,
                               String patchVersion, int count) {
        super();
        this.buddyId = buddyId;
        this.fromMessageId = fromMessageId;
        this.tillMessageId = tillMessageId;
        this.patchVersion = patchVersion;
        this.count = count;
    }

    @Override
    protected int parseResults(JSONObject results) throws Throwable {
        return REQUEST_DELETE;
    }

    @Override
    protected String getMethodName() {
        return "getHistory";
    }

    @Override
    protected void appendParams(JsonObject params) {
        String aimSid = getAccountRoot().getAimSid();
        String localeId = getAccountRoot().getLocaleId();
        params.addProperty("sn", buddyId);
        params.addProperty("fromMsgId", fromMessageId);
        params.addProperty("count", count);
        if (tillMessageId != 0) {
            params.addProperty("tillMsgId", tillMessageId);
        }
        params.addProperty("patchVersion", patchVersion == null || patchVersion.isEmpty() ? "init" : patchVersion);
        params.addProperty("aimSid", aimSid);
        params.addProperty("lang", localeId);
    }
}
