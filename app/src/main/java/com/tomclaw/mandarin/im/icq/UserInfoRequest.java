package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 25/03/15
 * Time: 7:49 PM
 */
public class UserInfoRequest extends WimRequest {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";

    public static final String NO_INFO_CASE = "no_info_case";
    public static final String EDIT_INFO_REQUEST = "edit_info_request";

    private String buddyId;

    @SuppressWarnings("unused")
    public UserInfoRequest() {
    }

    public UserInfoRequest(String buddyId) {
        this.buddyId = buddyId;
    }

    @Override
    protected JSONObject parseResponse(String responseString) throws JSONException {
        return super.parseResponse(StringUtil.fixCyrillicSymbols(responseString));
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BUDDY_ID, buddyId);
        // Start to JSON parsing.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            JSONObject data = responseObject.getJSONObject("data");
            JSONArray users = data.getJSONArray("users");
            if (users.length() > 0) {
                Context context = getAccountRoot().getContext();
                // Only first profile we need.
                JSONObject user = users.getJSONObject(0);
                String iconId = user.optString("iconId");
                String buddyIcon = user.optString("buddyIcon");
                String largeIconId = user.optString("largeIconId");
                // Check avatar fields be able to modify.
                if (!TextUtils.isEmpty(iconId) && !TextUtils.isEmpty(buddyIcon) && iconId.length() > 4 &&
                        !TextUtils.isEmpty(largeIconId) && largeIconId.length() > 4) {
                    // Cut four first bytes and replace icon type.
                    iconId = iconId.substring(4);
                    largeIconId = largeIconId.substring(4);
                    buddyIcon = buddyIcon.replace(iconId, largeIconId);
                    buddyIcon = buddyIcon.replace("buddyIcon", "largeBuddyIcon");
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
                JSONObject profile = user.optJSONObject("profile");
                // Sometimes profile may not present. Check it right now.
                if (profile == null) {
                    intent.putExtra(NO_INFO_CASE, true);
                } else {
                    // Obtain buddy info from profile.
                    putExtra(intent, R.id.friendly_name, profile.optString("friendlyName"));
                    putExtra(intent, R.id.first_name, profile.optString("firstName"));
                    putExtra(intent, R.id.last_name, profile.optString("lastName"));
                    String gender = profile.optString("gender");
                    if (!TextUtils.isEmpty(gender) && !TextUtils.equals(gender, "unknown")) {
                        int genderValue = gender.equals("male") ? 1 : 2;
                        putExtra(intent, R.id.gender, genderValue);
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
                            putExtra(intent, R.id.city, city);
                        }
                    }

                    putExtra(intent, R.id.website, profile.optString("website1"));
                    if (profile.has("birthDate")) {
                        long birthDate = profile.optLong("birthDate") * 1000;
                        putExtra(intent, R.id.birth_date, birthDate);
                    }
                    putExtra(intent, R.id.about_me, profile.optString("aboutMe"));
                }
            }
        } else {
            intent.putExtra(NO_INFO_CASE, true);
        }
        intent.putExtra(EDIT_INFO_REQUEST, true);
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("presence/get");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", WimConstants.FORMAT_JSON)
                .appendParam("infoLevel", "mid")
                .appendParam("t", buddyId)
                .appendParam("mdir", "1");
    }

    private void putExtra(Intent intent, int key, int value) {
        intent.putExtra(String.valueOf(key), value);
    }

    private void putExtra(Intent intent, int key, long value) {
        intent.putExtra(String.valueOf(key), value);
    }

    private void putExtra(Intent intent, int key, boolean value) {
        intent.putExtra(String.valueOf(key), value);
    }

    private void putExtra(Intent intent, int key, String value) {
        if (TextUtils.isEmpty(value.trim())) {
            return;
        }
        intent.putExtra(String.valueOf(key), value);
    }
}
