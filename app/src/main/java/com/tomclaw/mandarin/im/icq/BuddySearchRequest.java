package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.os.Bundle;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created by Igor on 26.06.2014.
 */
public class BuddySearchRequest extends WimRequest {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String SEARCH_OPTIONS = "search_options";

    public static final String SEARCH_RESULT_TOTAL = "search_result_total";
    public static final String SEARCH_RESULT_OFFSET = "search_result_offset";
    public static final String SEARCH_RESULT_BUNDLE = "search_result_bundle";
    public static final String NO_SEARCH_RESULT_CASE = "no_search_result_case";

    private IcqSearchOptionsBuilder searchOptions;
    private int nToGet;
    private int nToSkip;
    private String locale;

    public BuddySearchRequest() {
    }

    public BuddySearchRequest(IcqSearchOptionsBuilder searchOptions, int nToGet, int nToSkip, String locale) {
        this.searchOptions = searchOptions;
        this.nToGet = nToGet;
        this.nToSkip = nToSkip;
        this.locale = locale;
    }

    @Override
    protected JSONObject parseResponse(String responseString) throws JSONException {
        return super.parseResponse(Strings.fixCyrillicSymbols(responseString));
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        // Start to JSON parsing.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            JSONObject data = responseObject.getJSONObject("data");
            JSONObject results = data.getJSONObject("results");
            int total = results.getInt("nTotal");
            int skipped = results.getInt("nSkipped");
            int profiles = results.getInt("nProfiles");
            List<String> userIds = new ArrayList<>();
            if (profiles > 0) {
                JSONArray infoArray = results.getJSONArray("infoArray");
                for (int i = 0; i < infoArray.length(); i++) {
                    JSONObject buddyInfo = infoArray.getJSONObject(i);
                    JSONObject profile = buddyInfo.getJSONObject("profile");
                    // Obtain buddy id from profile.
                    String buddyId = profile.getString("aimId");
                    userIds.add(buddyId);
                }
            }
            // Delegate request to get buddies avatars.
            RequestHelper.requestBuddyPresence(getAccountRoot().getContentResolver(),
                    CoreService.getAppSession(), getAccountRoot().getAccountDbId(),
                    total, skipped, userIds, searchOptions);
        } else {
            // We must send intent in any case,
            // because our request is going to be deleted.
            getService().sendBroadcast(getNoResultIntent(
                    getAccountRoot().getAccountDbId(), searchOptions));
        }
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("memberDir/search");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", "json")
                .appendParam("infoLevel", "min")
                .appendParam("nToSkip", String.valueOf(nToSkip))
                .appendParam("nToGet", String.valueOf(nToGet))
                .appendParam("locale", locale)
                .appendParam("match", searchOptions.toString());
    }

    private static Intent getBaseIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions) {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(ACCOUNT_DB_ID, accountDbId);
        intent.putExtra(SEARCH_OPTIONS, searchOptions);
        return intent;
    }

    static Intent getNoResultIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions) {
        Intent intent = getBaseIntent(accountDbId, searchOptions);
        intent.putExtra(NO_SEARCH_RESULT_CASE, true);
        return intent;
    }

    static Intent getSearchResultIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions,
                                        int total, int skipped, Map<String, ShortBuddyInfo> shortInfoMap) {
        Intent intent = getBaseIntent(accountDbId, searchOptions);
        intent.putExtra(SEARCH_RESULT_TOTAL, total);
        intent.putExtra(SEARCH_RESULT_OFFSET, skipped);
        Bundle profilesBundle = new Bundle();
        for (String buddyId : shortInfoMap.keySet()) {
            profilesBundle.putSerializable(buddyId, shortInfoMap.get(buddyId));
        }
        intent.putExtra(SEARCH_RESULT_BUNDLE, profilesBundle);
        return intent;
    }
}
