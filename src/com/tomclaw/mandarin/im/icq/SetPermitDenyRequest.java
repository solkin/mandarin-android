package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;
import android.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by Solkin on 20.12.2015.
 */
public class SetPermitDenyRequest extends WimRequest {

    private String pdAllow;
    private String pdIgnore;
    private String pdBlock;
    private String pdAllowRemove;
    private String pdIgnoreRemove;
    private String pdBlockRemove;
    private String pdMode;

    public SetPermitDenyRequest() {
    }

    public void setPdAllow(String pdAllow) {
        this.pdAllow = pdAllow;
    }

    public void setPdIgnore(String pdIgnore) {
        this.pdIgnore = pdIgnore;
    }

    public void setPdBlock(String pdBlock) {
        this.pdBlock = pdBlock;
    }

    public void setPdAllowRemove(String pdAllowRemove) {
        this.pdAllowRemove = pdAllowRemove;
    }

    public void setPdIgnoreRemove(String pdIgnoreRemove) {
        this.pdIgnoreRemove = pdIgnoreRemove;
    }

    public void setPdBlockRemove(String pdBlockRemove) {
        this.pdBlockRemove = pdBlockRemove;
    }

    public void setPdMode(String pdMode) {
        this.pdMode = pdMode;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
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
                .concat("preference/setPermitDeny");
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<>("f", "json"));
        appendNonEmptyPair(params, "pdAllow", pdAllow);
        appendNonEmptyPair(params, "pdIgnore", pdIgnore);
        appendNonEmptyPair(params, "pdBlock", pdBlock);
        appendNonEmptyPair(params, "pdAllowRemove", pdAllowRemove);
        appendNonEmptyPair(params, "pdIgnoreRemove", pdIgnoreRemove);
        appendNonEmptyPair(params, "pdBlockRemove", pdBlockRemove);
        appendNonEmptyPair(params, "pdMode", pdMode);
        return params;
    }

    private void appendNonEmptyPair(List<Pair<String, String>> params, String title, String value) {
        if (!TextUtils.isEmpty(value)) {
            params.add(new Pair<>(title, value));
        }
    }
}
