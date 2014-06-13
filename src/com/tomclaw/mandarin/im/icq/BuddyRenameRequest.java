package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

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
        Collection<Integer> buddyDbIds;
        try {
            Map<String, Object> criteria = new HashMap<String, Object>();
            criteria.put(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_RENAME);
            buddyDbIds = QueryHelper.getBuddyDbIds(getAccountRoot().getContentResolver(),
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
                    buddyDbIds, buddyPreviousNick, false);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or McDonald's.
        return REQUEST_PENDING;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("buddylist/setBuddyAttribute");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("autoResponse", "false"));
        params.add(new Pair<String, String>("f", WimConstants.FORMAT_JSON));
        params.add(new Pair<String, String>("buddy", buddyId));
        params.add(new Pair<String, String>("friendly", buddySatisfiedNick));
        return params;
    }
}
