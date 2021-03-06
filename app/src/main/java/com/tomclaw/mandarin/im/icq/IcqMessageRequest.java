package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.REQUEST_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/12/13
 * Time: 1:38 PM
 */
public class IcqMessageRequest extends WimRequest {

    private String to;
    private String message;
    private String cookie;

    public IcqMessageRequest() {
    }

    public IcqMessageRequest(String to, String message, String cookie) {
        this.to = to;
        this.message = message;
        this.cookie = cookie;
    }

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.POST;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            return REQUEST_DELETE;
        } else if (statusCode >= 460 && statusCode <= 606) {
            // Target error. Mark message as error and delete request from pending operations.
            String requestId = responseObject.getString(REQUEST_ID);
            // TODO: Maybe, something?
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("im/sendIM");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("autoResponse", "false")
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("message", message)
                .appendParam("notifyDelivery", "true")
                .appendParam("offlineIM", "true")
                .appendParam("r", cookie)
                .appendParam("t", to);
    }
}
