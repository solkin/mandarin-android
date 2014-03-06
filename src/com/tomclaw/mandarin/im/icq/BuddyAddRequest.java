package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by solkin on 06/03/14.
 */
public class BuddyAddRequest extends WimRequest {

    private String buddyId;
    private String groupName;
    private String authMessage;

    public BuddyAddRequest(String buddyId, String groupName, String authMessage) {
        this.buddyId = buddyId;
        this.groupName = groupName;
        this.authMessage = authMessage;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            // TODO: here we must remove operation flag.
            return REQUEST_DELETE;
        } else if (statusCode == 462 || statusCode >= 600 && statusCode < 700) { // TODO: check this status codes.
            // TODO: here we must remove operation flag and move buddy info recycle group.
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or McDonald's.
        return REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("buddylist/addBuddy");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("buddy", buddyId));
        params.add(new Pair<String, String>("group", groupName));
        params.add(new Pair<String, String>("preAuthorized", "true"));
        params.add(new Pair<String, String>("authorizationMsg", authMessage));
        return params;
    }
}
