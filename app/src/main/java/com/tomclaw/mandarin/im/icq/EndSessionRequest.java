package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 21.10.13
 * Time: 0:20
 */
public class EndSessionRequest extends WimRequest {

    public EndSessionRequest() {
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK || statusCode == WIM_AUTH_REQUIRED) { // TODO: check for other status codes.
            // Session now ended or already ended.
            IcqAccountRoot accountRoot = getAccountRoot();
            accountRoot.resetSessionData();
            accountRoot.carriedOff();
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("aim/endSession");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON);
    }
}
