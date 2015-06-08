package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by solkin on 05/05/14.
 */
public class IcqTypingRequest extends WimRequest {

    private String buddyId;
    private boolean isTyping;

    public IcqTypingRequest(String buddyId, boolean isTyping) {
        this.buddyId = buddyId;
        this.isTyping = isTyping;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            return REQUEST_DELETE;
        }
        return REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("im/setTyping");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<>("f", WimConstants.FORMAT_JSON));
        params.add(new Pair<>("t", buddyId));
        params.add(new Pair<>("typingStatus", isTyping ? "typing" : "none"));
        return params;
    }
}
