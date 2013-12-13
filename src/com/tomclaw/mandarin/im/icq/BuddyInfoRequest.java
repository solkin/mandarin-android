package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.util.Pair;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.main.BuddyInfoActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/12/13
 * Time: 7:49 PM
 */
public class BuddyInfoRequest extends WimRequest {

    private String buddyId;

    public BuddyInfoRequest() {
    }

    public BuddyInfoRequest(String buddyId) {
        this.buddyId = buddyId;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        // Start to JSON parsing.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            JSONObject data = responseObject.getJSONObject("data");
            JSONArray infoArray = data.getJSONArray("infoArray");
            if(infoArray.length() > 0) {
                JSONObject firstProfile = infoArray.getJSONObject(0);
                JSONObject profile = firstProfile.getJSONObject("profile");
                // Obtain buddy info from profile.
                String aimId = profile.getString("aimId");

                intent.putExtra(BuddyInfoActivity.BUDDY_ID, aimId);
            }
        } else {
            intent.putExtra(BuddyInfoActivity.NO_INFO_CASE, true);
        }
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("memberDir/get");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("infoLevel", "min"));
        params.add(new Pair<String, String>("t", buddyId));
        return params;
    }
}
