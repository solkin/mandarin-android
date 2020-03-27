package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.text.TextUtils;

import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tomclaw.mandarin.im.icq.WimConstants.FALLBACK_STATE;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created by Solkin on 24.07.2014.
 */
public class BuddyPresenceRequest extends WimRequest {

    private int total;
    private int skipped;
    private List<String> buddyIds;
    private IcqSearchOptionsBuilder searchOptions;
    private boolean isKeywordNumeric = false;

    public BuddyPresenceRequest() {
    }

    public BuddyPresenceRequest(int total, int skipped, List<String> buddyIds,
                                IcqSearchOptionsBuilder searchOptions) {
        this.total = total;
        this.skipped = skipped;
        this.buddyIds = buddyIds;
        this.searchOptions = searchOptions;
    }

    @Override
    protected JSONObject parseResponse(String responseString) throws JSONException {
        return super.parseResponse(StringUtil.fixCyrillicSymbols(responseString));
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        Intent intent;
        // Start to JSON parsing.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            Map<String, ShortBuddyInfo> shortInfoMap = new HashMap<>();
            JSONObject data = responseObject.getJSONObject(WimConstants.DATA_OBJECT);
            JSONArray users = data.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject buddy = users.getJSONObject(i);
                JSONObject profile = buddy.optJSONObject("profile");
                String buddyId = buddy.getString(WimConstants.AIM_ID);
                if (profile != null) {
                    ShortBuddyInfo buddyInfo = new ShortBuddyInfo(buddyId);
                    String state = buddy.optString(WimConstants.STATE, FALLBACK_STATE);
                    String buddyIcon = HttpUtil.getAvatarUrl(buddyId);
                    int statusIndex;
                    try {
                        statusIndex = StatusUtil.getStatusIndex(getAccountRoot().getAccountType(), state);
                    } catch (StatusNotFoundException ignored) {
                        // Wow, this is a pretty strange status!
                        statusIndex = StatusUtil.STATUS_OFFLINE;
                    }
                    buddyInfo.setOnline(statusIndex != StatusUtil.STATUS_OFFLINE);
                    // Create custom avatar request.
                    if (!TextUtils.isEmpty(buddyIcon)) {
                        Logger.log("search avatar for " + buddyId + ": " + buddyIcon);
                        RequestHelper.requestSearchAvatar(getAccountRoot().getContentResolver(),
                                getAccountRoot().getAccountDbId(), buddyId,
                                CoreService.getAppSession(), buddyIcon);
                    }
                    // Parsing profile.
                    buddyInfo.setBuddyNick(profile.optString("friendlyName"));
                    buddyInfo.setFirstName(profile.optString("firstName"));
                    buddyInfo.setLastName(profile.optString("lastName"));
                    String gender = profile.optString("gender");
                    if (TextUtils.equals(gender, "female")) {
                        buddyInfo.setGender(Gender.Female);
                    } else if (TextUtils.equals(gender, "male")) {
                        buddyInfo.setGender(Gender.Male);
                    }
                    JSONArray homeAddress = profile.optJSONArray("homeAddress");
                    if (homeAddress != null) {
                        String city = "";
                        for (int c = 0; c < homeAddress.length(); c++) {
                            if (c > 0) {
                                city += ", ";
                            }
                            city += homeAddress.getJSONObject(c).optString("city");
                        }
                        if (!TextUtils.isEmpty(city)) {
                            buddyInfo.setHomeAddress(city);
                        }
                    }
                    if (profile.has("birthDate")) {
                        long birthDate = profile.optLong("birthDate") * 1000;
                        buddyInfo.setBirthDate(birthDate);
                    }
                    buddyInfo.setItemStatic(isKeywordNumeric && TextUtils.equals(buddyId, searchOptions.getKeyword()));
                    shortInfoMap.put(buddyId, buddyInfo);
                }
            }
            intent = BuddySearchRequest.getSearchResultIntent(getAccountRoot().getAccountDbId(),
                    searchOptions, total, skipped, shortInfoMap);
        } else {
            intent = BuddySearchRequest.getNoResultIntent(getAccountRoot().getAccountDbId(), searchOptions);
        }
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("presence/get");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        HttpParamsBuilder params = new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("mdir", "1");
        // Checking for keyword is user id and this is first result page.
        if (StringUtil.isNumeric(searchOptions.getKeyword()) && skipped == 0 &&
                !buddyIds.contains(searchOptions.getKeyword())) {
            appendBuddyPair(params, searchOptions.getKeyword());
            isKeywordNumeric = true;
        }
        for (String buddyId : buddyIds) {
            appendBuddyPair(params, buddyId);
        }
        return params;
    }

    private static void appendBuddyPair(HttpParamsBuilder builder, String buddyId) {
        builder.appendParam("t", buddyId);
    }
}
