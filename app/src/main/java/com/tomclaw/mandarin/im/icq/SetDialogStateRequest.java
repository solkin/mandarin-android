package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

import org.json.JSONObject;

/**
 * Created by ivsolkin on 28.11.16.
 */
public class SetDialogStateRequest extends CabbageTrueRequest {

    private String buddyId;
    private long messageId;

    public SetDialogStateRequest(String buddyId, long messageId) {
        this.buddyId = buddyId;
        this.messageId = messageId;
    }

    @Override
    protected int parseResults(JSONObject results) throws Throwable {
        return REQUEST_DELETE;
    }

    @Override
    protected String getMethodName() {
        return "setDlgStateWim";
    }

    @Override
    protected void appendParams(JsonObject params) {
        params.addProperty("sn", buddyId);
        params.addProperty("lastRead", messageId);
    }
}
