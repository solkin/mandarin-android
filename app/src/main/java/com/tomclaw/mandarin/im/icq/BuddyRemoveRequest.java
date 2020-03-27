package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.StrictBuddy;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created by Solkin on 07.06.2014.
 */
public class BuddyRemoveRequest extends WimRequest {

    private String buddyId;
    private String groupName;

    public BuddyRemoveRequest() {
    }

    public BuddyRemoveRequest(String groupName, String buddyId) {
        this.buddyId = buddyId;
        this.groupName = groupName;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        int accountDbId = getAccountRoot().getAccountDbId();
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        StrictBuddy strictBuddy = new StrictBuddy(accountDbId, groupName, buddyId);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            // Buddy will be removed later when it became outdated in roster.
            return REQUEST_DELETE;
        } else if (statusCode == 601) {
            // Buddy not found in roster.
            QueryHelper.removeBuddy(databaseLayer, strictBuddy);
            return REQUEST_DELETE;
        } else if (statusCode == 460 || statusCode == 462) {
            // No luck :( Return buddy.
            QueryHelper.modifyOperation(databaseLayer, strictBuddy,
                    GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("buddylist/removeBuddy");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("autoResponse", "false")
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("buddy", buddyId)
                .appendParam("group", groupName);
    }
}
