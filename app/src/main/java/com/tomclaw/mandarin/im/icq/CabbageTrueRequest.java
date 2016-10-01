package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

/**
 * Created by ivsolkin on 28.09.16.
 */
public abstract class CabbageTrueRequest extends CabbageRequest {

    private long clientId;

    public CabbageTrueRequest(String requestId, String authToken, long clientId) {
        super(requestId, authToken);
        this.clientId = clientId;
    }

    @Override
    protected void addProperty(JsonObject root) {
        root.addProperty("clientId", clientId);
    }
}
