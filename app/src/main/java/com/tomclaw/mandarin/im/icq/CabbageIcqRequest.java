package com.tomclaw.mandarin.im.icq;

import com.google.gson.JsonObject;

/**
 * Created by ivsolkin on 28.09.16.
 */
public abstract class CabbageIcqRequest extends CabbageRequest {

    public CabbageIcqRequest() {
        super();
    }

    protected void addProperty(JsonObject root) {
        root.addProperty("icqAkes", WimConstants.DEV_ID);
    }
}
