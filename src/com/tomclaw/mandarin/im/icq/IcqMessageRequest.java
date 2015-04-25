package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

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
        // Maybe incorrect aim sid or McDonald's.
        return REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("im/sendIM");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<>("autoResponse", "false"));
        params.add(new Pair<>("f", WimConstants.FORMAT_JSON));
        params.add(new Pair<>("message", message));
        params.add(new Pair<>("notifyDelivery", "true"));
        params.add(new Pair<>("offlineIM", "true"));
        params.add(new Pair<>("r", cookie));
        params.add(new Pair<>("t", to));
        return params;
    }
}
