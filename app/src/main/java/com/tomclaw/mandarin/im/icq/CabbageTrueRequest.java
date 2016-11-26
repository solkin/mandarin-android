package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

/**
 * Created by ivsolkin on 28.09.16.
 */
public abstract class CabbageTrueRequest extends CabbageRequest {

    public CabbageTrueRequest() {
        super();
    }

    @Override
    protected void addProperty(JsonObject root) {
        long clientId = getAccountRoot().getClientId();
        root.addProperty("clientId", clientId);
    }
}
