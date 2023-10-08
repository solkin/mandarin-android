package com.tomclaw.mandarin.im.icq;

import static com.tomclaw.mandarin.im.icq.WimConstants.CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESULTS_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_RAPI_BASE;
import static com.tomclaw.mandarin.util.StringUtil.generateRandomWord;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.JsonParamsBuilder;
import com.tomclaw.mandarin.util.ParamsBuilder;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    public BuddySearchRequest() {
    }

    public BuddySearchRequest(IcqSearchOptionsBuilder searchOptions, int nToGet, int nToSkip, String locale) {
        this.searchOptions = searchOptions;
    }

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.POST;
    }

    @Override
    protected JSONObject parseResponse(String responseString) throws JSONException {
        return super.parseResponse(StringUtil.fixCyrillicSymbols(responseString));
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        Intent intent;
        // Start to JSON parsing.
        JSONObject results = response.getJSONObject(RESULTS_OBJECT);
        JSONObject statusObject = response.getJSONObject(STATUS_OBJECT);
        int statusCode = statusObject.getInt(CODE);
        // Check for server reply.
        if (statusCode == CABBAGE_OK) {
            Map<String, ShortBuddyInfo> shortInfoMap = new HashMap<>();
            JSONArray persons = results.getJSONArray("persons");
            boolean finish = results.optBoolean("finish", false);
            if (persons.length() > 0) {
                IcqAccountRoot accountRoot = getAccountRoot();
                for (int i = 0; i < persons.length(); i++) {
                    JSONObject person = persons.getJSONObject(i);
                    String buddyId = person.getString("sn");
                    if (buddyId.equals(accountRoot.getUserId())) {
                        continue;
                    }
                    String friendly = person.optString("friendly");
                    String firstName = person.optString("firstName");
                    String lastName = person.optString("lastName");
                    String buddyIcon = HttpUtil.getAvatarUrl(buddyId);
                    String avatarHash = null;
                    // Check avatar fields be able to modify.
                    if (!TextUtils.isEmpty(buddyIcon)) {
                        avatarHash = HttpUtil.getUrlHash(buddyIcon);
                        Context context = accountRoot.getContext();
                        RequestHelper.requestLargeAvatar(
                                context.getContentResolver(), getAccountRoot().getAccountDbId(),
                                buddyId, CoreService.getAppSession(), buddyIcon);
                    }
                    ShortBuddyInfo buddyInfo = new ShortBuddyInfo(buddyId);
                    buddyInfo.setBuddyNick(friendly);
                    buddyInfo.setFirstName(firstName);
                    buddyInfo.setLastName(lastName);
                    buddyInfo.setAvatarHash(avatarHash);
                    shortInfoMap.put(buddyId, buddyInfo);
                }
            }
            intent = BuddySearchRequest.getSearchResultIntent(getAccountRoot().getAccountDbId(),
                    searchOptions, 0, 0, shortInfoMap);
        } else {
            // We must send intent in any case,
            // because our request is going to be deleted.
            intent = BuddySearchRequest.getNoResultIntent(getAccountRoot().getAccountDbId(), searchOptions);
        }
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return WEB_RAPI_BASE.concat("v92/rapi/search");
    }

    @Override
    public Map<String, String> getRequestProperties() {
        return Collections.singletonMap("Content-Type", "application/json;charset=UTF-8");
    }

    @Override
    protected ParamsBuilder getParams() {
        JsonParamsBuilder builder = new JsonParamsBuilder();
        try {
            builder.put("reqId", generateRandomWord());
            builder.put("aimsid", getAccountRoot().getAimSid());
            JSONObject params = new JSONObject();
            params.put("keyword", searchOptions.getKeyword());
            builder.put("params", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder;
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
