package com.tomclaw.mandarin.im.icq;

import com.tomclaw.helpers.StringUtil;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.GregorianCalendar;

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
    private String city;
    private String webSite;
    private String aboutMe;

    public UpdateInfoRequest(String friendlyName, String firstName, String lastName, int gender,
                             long birthDate, String city, String webSite, String aboutMe) {
        this.friendlyName = friendlyName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
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
    protected HttpParamsBuilder getParams() {
        String genderString;
        if (gender == 1) {
            genderString = "male";
        } else if (gender == 2) {
            genderString = "female";
        } else {
            genderString = "unknown";
        }

        HttpParamsBuilder params = new HttpParamsBuilder();
        params.appendParam("aimsid", getAccountRoot().getAimSid());
        params.appendParam("f", WimConstants.FORMAT_JSON);
        try {
            params.appendParam("set", getFieldValue("friendlyName", friendlyName));
            params.appendParam("set", getFieldValue("firstName", firstName));
            params.appendParam("set", getFieldValue("lastName", lastName));
            params.appendParam("set", getFieldValue("gender", genderString));
            params.appendParam("set", getPairValue("homeAddress", "city", city));
            params.appendParam("set", getFieldValue("website1", webSite));
            if (birthDate > new GregorianCalendar(0, 0, 0).getTimeInMillis()) {
                params.appendParam("set", getFieldValue("birthDate", birthDate / 1000));
            }
            params.appendParam("set", getFieldValue("aboutMe", aboutMe));
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
