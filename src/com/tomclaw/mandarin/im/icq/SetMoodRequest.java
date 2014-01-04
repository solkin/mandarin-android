package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.util.Pair;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.AccountInfoActivity;
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
public class SetMoodRequest extends WimRequest {

    public static final transient int STATUS_MOOD_RESET = -1;

    private int statusIndex;

    public SetMoodRequest(int statusIndex) {
        this.statusIndex = statusIndex;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        boolean isSetStateSuccess = false;
        // Prepare intent for activity.
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(AccountInfoActivity.ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(AccountInfoActivity.STATE_REQUESTED, statusIndex);
        // Parsing response.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            isSetStateSuccess = true;
        }
        intent.putExtra(AccountInfoActivity.SET_STATE_SUCCESS, isSetStateSuccess);
        // Maybe incorrect aim sid or McDonald's.
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("presence/setStatus");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        String statusValue;
        // Checking for this is mood reset.
        if(statusIndex == STATUS_MOOD_RESET) {
            statusValue = "";
        } else {
            statusValue = StatusUtil.getStatusValue(getAccountRoot().getAccountType(), statusIndex);
        }

        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("mood", statusValue));
        params.add(new Pair<String, String>("title", ""));
        params.add(new Pair<String, String>("statusMsg", ""));
        return params;
    }
}
