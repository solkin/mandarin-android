package com.tomclaw.mandarin.im.icq;

import static com.tomclaw.mandarin.im.icq.WimConstants.CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESULTS_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_RAPI_BASE;
import static com.tomclaw.mandarin.util.StringUtil.generateRandomWord;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.tomclaw.mandarin.util.JsonParamsBuilder;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.ParamsBuilder;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/12/13
 * Time: 7:49 PM
 */
public class BuddyInfoRequest extends WimRequest {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_NICK = "buddy_nick";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";
    public static final String BUDDY_STATUS = "buddy_status";
    public static final String BUDDY_IGNORED = "buddy_ignored";

    public static final String NO_INFO_CASE = "no_info_case";
    public static final String INFO_RESPONSE = "info_response";

    public static final String BUDDY_STATUS_TITLE = "buddy_status_title";
    public static final String BUDDY_STATUS_MESSAGE = "buddy_status_message";

    private String buddyId;

    /**
     * Date format helper
     */
    private static final transient DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();

    public BuddyInfoRequest() {
    }

    public BuddyInfoRequest(String buddyId) {
        this.buddyId = buddyId;
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
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(INFO_RESPONSE, true);
        intent.putExtra(ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BUDDY_ID, buddyId);
        // Start to JSON parsing.
        JSONObject results = response.getJSONObject(RESULTS_OBJECT);
        JSONObject statusObject = response.getJSONObject(STATUS_OBJECT);
        int statusCode = statusObject.getInt(CODE);
        // Check for server reply.
        if (statusCode == CABBAGE_OK) {
            Context context = getAccountRoot().getContext();
            String buddyIcon = HttpUtil.getAvatarUrl(buddyId);
            // Check avatar fields be able to modify.
            if (!TextUtils.isEmpty(buddyIcon)) {
                String hash = HttpUtil.getUrlHash(buddyIcon);
                Logger.log("large buddy icon: " + buddyIcon);
                // Check for such avatar is already loaded.
                String avatarHash;
                try {
                    avatarHash = QueryHelper.getBuddyOrAccountAvatarHash(getAccountRoot(), buddyId);
                } catch (AccountNotFoundException | BuddyNotFoundException ignored) {
                    // No buddy - no avatar.
                    avatarHash = null;
                }
                if (TextUtils.equals(avatarHash, hash)) {
                    QueryHelper.updateBuddyOrAccountAvatar(getAccountRoot(), buddyId, hash);
                    intent.putExtra(BUDDY_AVATAR_HASH, hash);
                } else {
                    RequestHelper.requestLargeAvatar(
                            context.getContentResolver(), getAccountRoot().getAccountDbId(),
                            buddyId, CoreService.getAppSession(), buddyIcon);
                }
            }
            // Obtain buddy info from profile.
            putExtra(intent, R.id.friendly_name, R.string.friendly_name, results.optString("friendly"));
            putExtra(intent, R.id.first_name, R.string.first_name, results.optString("firstName"));
            putExtra(intent, R.id.last_name, R.string.last_name, results.optString("lastName"));
            String gender = results.optString("gender");
            if (!TextUtils.isEmpty(gender) && !TextUtils.equals(gender, "unknown")) {
                String genderString = gender.equals("male") ?
                        context.getString(R.string.male) : context.getString(R.string.female);
                putExtra(intent, R.id.gender, R.string.gender, genderString);
            }
            JSONArray homeAddress = results.optJSONArray("homeAddress");
            if (homeAddress != null) {
                String city = "";
                for (int c = 0; c < homeAddress.length(); c++) {
                    if (c > 0) {
                        city += ", ";
                    }
                    city += homeAddress.getJSONObject(c).optString("city");
                }
                if (!TextUtils.isEmpty(city)) {
                    putExtra(intent, R.id.city, R.string.city, city);
                }
            }

            putExtra(intent, R.id.website, R.string.website, results.optString("website1"));
            if (results.has("birthDate")) {
                long birthDate = results.optLong("birthDate") * 1000;
                putExtra(intent, R.id.birth_date, R.string.birth_date, DATE_FORMAT.format(birthDate));
            }
            putExtra(intent, R.id.about_me, R.string.about_me, results.optString("about"));

        } else {
            intent.putExtra(NO_INFO_CASE, true);
        }
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return WEB_RAPI_BASE.concat("v92/rapi/getUserInfo");
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
            params.put("sn", buddyId);
            builder.put("params", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder;
    }

    private void putExtra(Intent intent, int key, int title, long value) {
        putExtra(intent, key, title, String.valueOf(value));
    }

    private void putExtra(Intent intent, int key, int title, boolean value) {
        Context context = getAccountRoot().getContext();
        putExtra(intent, key, title, context.getString(value ? R.string.info_yes : R.string.info_no));
    }

    private void putExtra(Intent intent, int key, int title, String value) {
        if (TextUtils.isEmpty(value.trim())) {
            return;
        }
        intent.putExtra(String.valueOf(key), new String[]{String.valueOf(title), value});
    }
}
