package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.util.Pair;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 1/4/14
 * Time: 5:34 PM
 */
public class SetStateRequest extends WimRequest {

    public static final String STATE_REQUESTED = "state_requested";
    public static final String STATE_APPLIED = "state_applied";
    public static final String SET_STATE_SUCCESS = "set_state_success";

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
            JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
            JSONObject myInfoObject = dataObject.getJSONObject(MY_INFO);
            String state = myInfoObject.getString(STATE);
            try {
                int statusIndexApplied = StatusUtil.getStatusIndex(getAccountRoot().getAccountType(), state);
                // Check for status setup was fully correct and state successfully applied.
                if (statusIndexApplied == statusIndex) {
                    isSetStateSuccess = true;
                } else {
                    intent.putExtra(STATE_APPLIED, statusIndexApplied);
                }
            } catch (StatusNotFoundException ignored) {
                // No such state? Hm... Really strange default state.
            }
        }
        intent.putExtra(SET_STATE_SUCCESS, isSetStateSuccess);
        // Maybe incorrect aim sid or McDonald's.
        return isRequestOk ? REQUEST_DELETE : REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("presence/setState");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        String statusValue = StatusUtil.getStatusValue(getAccountRoot().getAccountType(), statusIndex);

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<>("f", WimConstants.FORMAT_JSON));
        params.add(new Pair<>("view", statusValue));
        params.add(new Pair<>("away", ""));
        return params;
    }
}
