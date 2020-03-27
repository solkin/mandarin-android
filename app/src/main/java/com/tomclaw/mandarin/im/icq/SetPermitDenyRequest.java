package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.HttpParamsBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

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
        return WEB_API_BASE.concat("preference/setPermitDeny");
    }

    @Override
    protected HttpParamsBuilder getParams() {
        return new HttpParamsBuilder()
                .appendParam("aimsid", getAccountRoot().getAimSid())
                .appendParam("f", "json")
                .appendParamNonEmpty("pdAllow", pdAllow)
                .appendParamNonEmpty("pdAllow", pdAllow)
                .appendParamNonEmpty("pdIgnore", pdIgnore)
                .appendParamNonEmpty("pdBlock", pdBlock)
                .appendParamNonEmpty("pdAllowRemove", pdAllowRemove)
                .appendParamNonEmpty("pdIgnoreRemove", pdIgnoreRemove)
                .appendParamNonEmpty("pdBlockRemove", pdBlockRemove)
                .appendParamNonEmpty("pdMode", pdMode);
    }
}
