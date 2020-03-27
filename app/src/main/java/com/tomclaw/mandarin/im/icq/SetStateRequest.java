package com.tomclaw.mandarin.im.icq;

import android.content.Intent;

import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.MY_INFO;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 1/4/14
 * Time: 5:34 PM
 */
public class SetStateRequest extends WimRequest {

    static final String STATE_REQUESTED = "state_requested";
    private static final String STATE_APPLIED = "state_applied";
    static final String SET_STATE_SUCCESS = "set_state_success";

    private int statusIndex;

    public SetStateRequest(int statusIndex) {
        this.statusIndex = statusIndex;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        boolean isSetStateSuccess = false;
        // Prepare intent for activity.
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(STATE_REQUESTED, statusIndex);
        // Parsing response.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        boolean isRequestOk = false;
        // Check for server reply.
        if (statusCode == WIM_OK) {
            isRequestOk = true;
            isSetStateSuccess = true;
        }
        intent.putExtra(SET_STATE_SUCCESS, isSetStateSuccess);
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return isRequestOk ? REQUEST_DELETE : REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("presence/setState");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        String statusValue = StatusUtil.getStatusValue(getAccountRoot().getAccountType(), statusIndex);

        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("view", statusValue)
                .appendParam("away", "");
    }
}
