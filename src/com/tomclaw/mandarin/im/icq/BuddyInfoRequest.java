package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.main.BuddyInfoActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/12/13
 * Time: 7:49 PM
 */
public class BuddyInfoRequest extends WimRequest {

    private String buddyId;

    /**
     * Date format helper
     */
    private static final transient SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

    public BuddyInfoRequest() {
    }

    public BuddyInfoRequest(String buddyId) {
        this.buddyId = buddyId;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(BuddyInfoActivity.ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BuddyInfoActivity.BUDDY_ID, buddyId);
        // Start to JSON parsing.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            JSONObject data = responseObject.getJSONObject("data");
            JSONArray infoArray = data.getJSONArray("infoArray");
            if (infoArray.length() > 0) {
                Context context = getAccountRoot().getContext();

                JSONObject firstProfile = infoArray.getJSONObject(0);
                JSONObject profile = firstProfile.getJSONObject("profile");
                // Obtain buddy info from profile.
                putExtra(intent, R.id.friendly_name, R.string.friendly_name, profile.optString("friendlyName"));
                putExtra(intent, R.id.first_name, R.string.first_name, profile.optString("firstName"));
                putExtra(intent, R.id.last_name, R.string.last_name, profile.optString("lastName"));
                String gender = profile.optString("gender");
                if (!TextUtils.equals(gender, "unknown")) {
                    String genderString = gender.equals("male") ?
                            context.getString(R.string.male) : context.getString(R.string.female);
                    putExtra(intent, R.id.gender, R.string.gender, genderString);
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
                        putExtra(intent, R.id.city, R.string.city, city);
                    }
                }

                int childrenCount = profile.optInt("children");
                if (childrenCount > 0) {
                    putExtra(intent, R.id.children, R.string.children, childrenCount);
                } else {
                    putExtra(intent, R.id.children, R.string.children, false);
                }
                putExtra(intent, R.id.smoking, R.string.smoking, profile.optBoolean("smoking"));
                putExtra(intent, R.id.website, R.string.website, profile.optString("website1"));
                long birthDate = profile.optLong("birthDate") * 1000;
                if (birthDate > 0) {
                    putExtra(intent, R.id.birth_date, R.string.birth_date, simpleDateFormat.format(birthDate));
                }
                putExtra(intent, R.id.about_me, R.string.about_me, profile.optString("aboutMe"));
            }
        } else {
            intent.putExtra(BuddyInfoActivity.NO_INFO_CASE, true);
        }
        // We must send intent in any case,
        // because our request is going to be deleted.
        getService().sendBroadcast(intent);
        return REQUEST_DELETE;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("memberDir/get");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("infoLevel", "mid"));
        params.add(new Pair<String, String>("t", buddyId));
        return params;
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
