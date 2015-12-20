package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by Igor on 05.04.2015.
 */
public class UpdateInfoRequest extends WimRequest {

    private String friendlyName;
    private String firstName;
    private String lastName;
    private int gender;
    private long birthDate;
    private int childrenCount;
    private boolean smoking;
    private String city;
    private String webSite;
    private String aboutMe;

    public UpdateInfoRequest(String friendlyName, String firstName, String lastName, int gender, long birthDate,
                             int childrenCount, boolean smoking, String city, String webSite, String aboutMe) {
        this.friendlyName = friendlyName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.childrenCount = childrenCount;
        this.smoking = smoking;
        this.city = city;
        this.webSite = webSite;
        this.aboutMe = aboutMe;
    }

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.POST;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        // Parsing response.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("memberDir/update");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        String genderString;
        if (gender == 1) {
            genderString = "male";
        } else if (gender == 2) {
            genderString = "female";
        } else {
            genderString = "unknown";
        }

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<>("f", WimConstants.FORMAT_JSON));
        try {
            params.add(new Pair<>("set", getFieldValue("friendlyName", friendlyName)));
            params.add(new Pair<>("set", getFieldValue("firstName", firstName)));
            params.add(new Pair<>("set", getFieldValue("lastName", lastName)));
            params.add(new Pair<>("set", getFieldValue("gender", genderString)));
            params.add(new Pair<>("set", getPairValue("homeAddress", "city", city)));
            params.add(new Pair<>("set", getFieldValue("children", childrenCount)));
            params.add(new Pair<>("set", getFieldValue("smoking", smoking)));
            params.add(new Pair<>("set", getFieldValue("website1", webSite)));
            if (birthDate > new GregorianCalendar(0, 0, 0).getTimeInMillis()) {
                params.add(new Pair<>("set", getFieldValue("birthDate", birthDate / 1000)));
            }
            params.add(new Pair<>("set", getFieldValue("aboutMe", aboutMe)));
        } catch (UnsupportedEncodingException ignored) {
            // Never come here.
        }
        return params;
    }

    private String getFieldValue(String field, boolean value) throws UnsupportedEncodingException {
        return getFieldValue(field, value ? "1" : "0");
    }

    private String getFieldValue(String field, long value) throws UnsupportedEncodingException {
        return getFieldValue(field, String.valueOf(value));
    }

    private String getFieldValue(String field, int value) throws UnsupportedEncodingException {
        return getFieldValue(field, String.valueOf(value));
    }

    private String getFieldValue(String field, String value) throws UnsupportedEncodingException {
        return field + "=" + StringUtil.urlEncode(value);
    }

    private String getPairValue(String field, String key, String value) throws UnsupportedEncodingException {
        return field + "=[{" + key + "=" + StringUtil.urlEncode(value) + "}]";
    }
}
