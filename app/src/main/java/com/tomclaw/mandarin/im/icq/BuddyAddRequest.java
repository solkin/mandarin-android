package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by Solkin on 27.07.2014.
 */
public class BuddyAddRequest extends WimRequest {

    private String buddyId;
    private String groupName;
    private String authorizationMsg;

    public BuddyAddRequest() {
    }

    public BuddyAddRequest(String buddyId, String groupName, String authorizationMsg) {
        this.buddyId = buddyId;
        this.groupName = groupName;
        this.authorizationMsg = authorizationMsg;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Searching for local buddy db id.
        int buddyDbId;
        try {
            buddyDbId = QueryHelper.getBuddyDbId(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), groupName, buddyId);
        } catch (BuddyNotFoundException ignored) {
            // Wha-a-a-at?! No buddy found. Maybe, it was deleted or never exists?
            // Heh, delete request.
            return REQUEST_DELETE;
        }
        // Check for server reply.
        if (statusCode == WIM_OK) {
            // We'll delete rename label later, when roster
            // with satisfied nick will be received.
            return REQUEST_DELETE;
        } else if (statusCode == 460 || statusCode == 462) {
            // No luck :( Move buddy into recycle.
            QueryHelper.moveBuddyIntoRecycle(getAccountRoot().getContentResolver(), getAccountRoot().getResources(),
                    buddyDbId);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("buddylist/addBuddy");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("buddy", buddyId)
                .appendParam("group", groupName)
                .appendParam("preAuthorized", "1")
                .appendParam("authorizationMsg", authorizationMsg)
                .appendParam("locationGroup", "0");
    }
}
