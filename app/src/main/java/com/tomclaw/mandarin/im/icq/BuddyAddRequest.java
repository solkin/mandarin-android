package com.tomclaw.mandarin.im.icq;

import android.content.res.Resources;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.StrictBuddy;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

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
        StrictBuddy buddy = new StrictBuddy(getAccountRoot().getAccountDbId(), groupName, buddyId);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            // We'll delete rename label later, when roster
            // with satisfied nick will be received.
            return REQUEST_DELETE;
        } else if (statusCode == 460 || statusCode == 462) {
            // No luck :( Move buddy into recycle.
            Resources resources = getAccountRoot().getResources();
            DatabaseLayer databaseLayer = getDatabaseLayer();
            QueryHelper.moveBuddyIntoRecycle(databaseLayer, resources, buddy);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("buddylist/addBuddy");
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
