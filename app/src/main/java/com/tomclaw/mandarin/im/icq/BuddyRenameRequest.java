package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by Solkin on 07.06.2014.
 */
public class BuddyRenameRequest extends WimRequest {

    private String buddyId;
    private String buddyPreviousNick;
    private String buddySatisfiedNick;

    public BuddyRenameRequest() {
    }

    public BuddyRenameRequest(String buddyId, String buddyPreviousNick, String buddySatisfiedNick) {
        this.buddyId = buddyId;
        this.buddyPreviousNick = buddyPreviousNick;
        this.buddySatisfiedNick = buddySatisfiedNick;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Searching for local buddy db id with rename operation label.
        Collection<Buddy> buddies;
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_RENAME);
            buddies = QueryHelper.getBuddies(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId, criteria);
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
            // No luck :( Return previous nick.
            QueryHelper.modifyBuddyNick(getAccountRoot().getContentResolver(),
                    buddies, buddyPreviousNick, false);
            return REQUEST_DELETE;
        } else if (statusCode == 601) {
            // Buddy not found in roster.
            // Set satisfied nick, remove operation flag and it will be done for now.
            QueryHelper.modifyBuddyNick(getAccountRoot().getContentResolver(),
                    buddies, buddySatisfiedNick, false);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("buddylist/setBuddyAttribute");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("autoResponse", "false")
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("buddy", buddyId)
                .appendParam("friendly", buddySatisfiedNick);
    }
}
