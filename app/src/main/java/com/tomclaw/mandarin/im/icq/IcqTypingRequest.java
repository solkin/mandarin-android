package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

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
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("im/setTyping");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("t", buddyId)
                .appendParam("typingStatus", isTyping ? "typing" : "none");
    }
}
