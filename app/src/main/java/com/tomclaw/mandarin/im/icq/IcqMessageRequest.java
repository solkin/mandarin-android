package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.IM_STATES;
import static com.tomclaw.mandarin.im.icq.WimConstants.MSG_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.REQUEST_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

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
            String requestId = responseObject.getString(REQUEST_ID);
            JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
            String state = dataObject.getString(STATE);
            String msgId = dataObject.getString(MSG_ID);
            // This will mark message with server-side msgId
            // to provide message stated in fetch events.
            QueryHelper.addMessageCookie(getAccountRoot().getContentResolver(), requestId, msgId);
            // Checking for message state.
            int messageState = GlobalProvider.HISTORY_MESSAGE_STATE_UNDETERMINED;
            for (int i = 0; i < IM_STATES.length; i++) {
                if (state.equals(IM_STATES[i])) {
                    messageState = i;
                    break;
                }
            }
            if (messageState < GlobalProvider.HISTORY_MESSAGE_STATE_SENT
                    && messageState != GlobalProvider.HISTORY_MESSAGE_STATE_ERROR) {
                messageState = GlobalProvider.HISTORY_MESSAGE_STATE_SENT;
            }
            QueryHelper.updateMessageState(getAccountRoot().getContentResolver(), messageState, requestId, msgId);
            return REQUEST_DELETE;
        } else if (statusCode >= 460 && statusCode <= 606) {
            // Target error. Mark message as error and delete request from pending operations.
            String requestId = responseObject.getString(REQUEST_ID);
            QueryHelper.updateMessageState(getAccountRoot().getContentResolver(),
                    GlobalProvider.HISTORY_MESSAGE_STATE_ERROR, requestId);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("im/sendIM");
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
