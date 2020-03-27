package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

/**
 * Created by Solkin on 20.12.2015.
 */
public abstract class GetPermitDenyRequest extends WimRequest {

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK) {
            JSONObject data = responseObject.getJSONObject("data");
            String pdMode = data.getString("pdMode");
            List<String> allows = jsonArrayToStringList(data.optJSONArray("allows"));
            List<String> blocks = jsonArrayToStringList(data.optJSONArray("blocks"));
            List<String> ignores = jsonArrayToStringList(data.optJSONArray("ignores"));
            onPermitDenyInfoReceived(pdMode, allows, blocks, ignores);
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    private List<String> jsonArrayToStringList(JSONArray jsonArray) throws JSONException {
        List<String> result = new ArrayList<>();
        if (jsonArray != null) {
            for (int c = 0; c < jsonArray.length(); c++) {
                result.add(jsonArray.getString(c));
            }
        }
        return result;
    }

    @Override
    protected String getUrl() {
        return WEB_API_BASE.concat("preference/getPermitDeny");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", "json");
    }

    protected abstract void onPermitDenyInfoReceived(String pdMode, List<String> allows, List<String> blocks,
                                                     List<String> ignores);
}
