package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.text.TextUtils;

import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 1/4/14
 * Time: 5:34 PM
 */
public class SetMoodRequest extends WimRequest {

    public static final transient int STATUS_MOOD_RESET = -1;
    public static final transient String STATUS_TEXT_EMPTY = "";

    private final int statusIndex;
    private String statusTitle;
    private String statusMessage;

    public SetMoodRequest(int statusIndex, String statusTitle, String statusMessage) {
        this.statusIndex = statusIndex;
        this.statusTitle = statusTitle;
        this.statusMessage = statusMessage;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        boolean isSetStateSuccess = false;
        // Prepare intent for activity.
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(SetStateRequest.STATE_REQUESTED, statusIndex);
        // Parsing response.
        JSONObject responseObject = response.optJSONObject(RESPONSE_OBJECT);
        if (responseObject == null) {
            return REQUEST_SKIP;
        }
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            isSetStateSuccess = true;
        }
        intent.putExtra(SetStateRequest.SET_STATE_SUCCESS, isSetStateSuccess);
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return isSetStateSuccess ? REQUEST_DELETE : REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("presence/setStatus");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        String statusValue;
        // Checking for this is mood reset.
        if (statusIndex == STATUS_MOOD_RESET) {
            statusValue = "";
        } else {
            statusValue = StatusUtil.getStatusValue(getAccountRoot().getAccountType(), statusIndex);
        }
        // Validating status texts.
        statusTitle = validateString(statusTitle);
        statusMessage = validateString(statusMessage);

        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("mood", statusValue)
                .appendParam("title", statusTitle)
                .appendParam("statusMsg", statusMessage);
    }

    private String validateString(String string) {
        if (TextUtils.isEmpty(string)) {
            return STATUS_TEXT_EMPTY;
        } else {
            return string;
        }
    }
}
