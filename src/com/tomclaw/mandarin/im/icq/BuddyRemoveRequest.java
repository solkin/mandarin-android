package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

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
        if(statusCode == WIM_OK) {
            // Buddy will be removed later when it became outdated in roster.
            return REQUEST_DELETE;
        } else if(statusCode == 460 || statusCode == 462) {
            // No luck :( Return buddy.
            QueryHelper.modifyOperation(getAccountRoot().getContentResolver(),
                    buddyDbId, GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or McDonald's.
        return REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("buddylist/removeBuddy");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("autoResponse", "false"));
        params.add(new Pair<String, String>("f", WimConstants.FORMAT_JSON));
        params.add(new Pair<String, String>("buddy", buddyId));
        params.add(new Pair<String, String>("group", groupName));
        return params;
    }
}
