package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.mandarin.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

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
            Map<String, ShortBuddyInfo> shortInfoMap = new HashMap<String, ShortBuddyInfo>();
            if(profiles > 0) {
                JSONArray infoArray = results.getJSONArray("infoArray");
                for (int i = 0; i < infoArray.length(); i++) {
                    JSONObject buddyInfo = infoArray.getJSONObject(i);
                    ShortBuddyInfo info = new ShortBuddyInfo();

                    JSONObject profile = buddyInfo.getJSONObject("profile");
                    // Obtain buddy info from profile.
                    String buddyId = profile.getString("aimId");
                    info.setBuddyId(buddyId);
                    info.setBuddyNick(StringUtil.fixCyrillicSymbols(profile.optString("friendlyName")));
                    info.setFirstName(StringUtil.fixCyrillicSymbols(profile.optString("firstName")));
                    info.setLastName(StringUtil.fixCyrillicSymbols(profile.optString("lastName")));
                    String gender = profile.optString("gender");
                    if (!TextUtils.equals(gender, "unknown")) {
                        info.setGender(gender.equals("male") ? Gender.Male : Gender.Female);
                    }
                    JSONArray homeAddress = profile.optJSONArray("homeAddress");
                    if (homeAddress != null) {
                        String city = "";
                        for (int c = 0; c < homeAddress.length(); c++) {
                            if (c > 0) {
                                city += ", ";
                            }
                            city += StringUtil.fixCyrillicSymbols(homeAddress.getJSONObject(c).optString("city"));
                        }
                        if (!TextUtils.isEmpty(city)) {
                            info.setHomeAddress(city);
                        }
                    }
                    long birthDate = profile.optLong("birthDate") * 1000;
                    if (birthDate > 0) {
                        info.setBirthDate(birthDate);
                    }
                    info.setOnline(TextUtils.equals(profile.optString("online"), "true"));
                    shortInfoMap.put(buddyId, info);
                }
            }
            // Delegate request to get buddies avatars.
            RequestHelper.requestBuddyPresence(getAccountRoot().getContentResolver(),
                    CoreService.getAppSession(), getAccountRoot().getAccountDbId(),
                    total, skipped, shortInfoMap, searchOptions);
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
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("memberDir/search");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("infoLevel", "mid"));
        params.add(new Pair<String, String>("nToSkip", String.valueOf(nToSkip)));
        params.add(new Pair<String, String>("nToGet", String.valueOf(nToGet)));
        params.add(new Pair<String, String>("locale", locale));
        params.add(new Pair<String, String>("match", searchOptions.toString()));
        return params;
    }

    private static Intent getBaseIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions) {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(ACCOUNT_DB_ID, accountDbId);
        intent.putExtra(SEARCH_OPTIONS, searchOptions);
        return intent;
    }

    public static Intent getNoResultIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions) {
        Intent intent = getBaseIntent(accountDbId, searchOptions);
        intent.putExtra(NO_SEARCH_RESULT_CASE, true);
        return intent;
    }

    public static Intent getSearchResultIntent(int accountDbId, IcqSearchOptionsBuilder searchOptions,
                                               int total, int skipped, Map<String, ShortBuddyInfo> shortInfoMap) {
        Intent intent = getBaseIntent(accountDbId, searchOptions);
        intent.putExtra(SEARCH_RESULT_TOTAL, total);
        intent.putExtra(SEARCH_RESULT_OFFSET, skipped);
        Bundle profilesBundle = new Bundle();
        for(String buddyId : shortInfoMap.keySet()) {
            profilesBundle.putSerializable(buddyId, shortInfoMap.get(buddyId));
        }
        intent.putExtra(SEARCH_RESULT_BUNDLE, profilesBundle);
        return intent;
    }
}
