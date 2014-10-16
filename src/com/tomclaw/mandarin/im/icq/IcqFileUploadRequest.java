package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.RangedUploadRequest;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.ServerInternalException;
import com.tomclaw.mandarin.core.exceptions.UnauthorizedException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Solkin on 14.10.2014.
 */
public class IcqFileUploadRequest extends RangedUploadRequest<IcqAccountRoot> {

    private static final String CLIENT_NAME = "ICQ";
    private final String INIT_URL = "http://files.icq.com/files/init";

    public IcqFileUploadRequest() {
    }

    public IcqFileUploadRequest(String path) {
        super(path);
    }

    @Override
    protected void onSuccess(String response) throws Throwable {
        JSONObject rootObject = new JSONObject(response);
        int status = rootObject.getInt("status");
        switch(status) {
            case 200: {
                JSONObject dataObject = rootObject.getJSONObject("data");
                int previewable = dataObject.getInt("is_previewable");
                String fileId = dataObject.getString("fileid");
                long fileSize = dataObject.getLong("filesize");
                String fileName = dataObject.getString("filename");
                String mime = dataObject.getString("mime");
                String staticUrl = dataObject.getString("static_url");
                Log.d(Settings.LOG_TAG, "onSuccess: " + staticUrl);
                break;
            }
            default: {
                identifyErrorResponse(status);
            }
        }
    }

    @Override
    protected void onFail() {
        Log.d(Settings.LOG_TAG, "onFail");
    }

    @Override
    public void onFileNotFound() {
        Log.d(Settings.LOG_TAG, "onFileNotFound: " + getPath());

    }

    @Override
    protected void onBufferReleased(long sent, long size) {
        Log.d(Settings.LOG_TAG, "onBufferReleased " + sent + "/" + size);
    }

    @Override
    protected String getUrl(String name, long size) throws Throwable {
        String params = "a" + "=" + StringUtil.urlEncode(getAccountRoot().getTokenA())
                + "&" + "aimsid" + "=" + StringUtil.urlEncode(getAccountRoot().getAimSid())
                + "&" + "client" + "=" + CLIENT_NAME
                + "&" + "f" + "=" + "json"
                + "&" + "filename" + "=" + StringUtil.urlEncode(name)
                + "&" + "k" + "=" + StringUtil.urlEncode(IcqSession.DEV_ID_VALUE)
                + "&" + "size" + "=" + size
                + "&" + "ts" + "=" + System.currentTimeMillis() / 1000;

        String hash = HttpUtil.GET.concat(WimConstants.AMP).concat(StringUtil.urlEncode(INIT_URL))
                .concat(WimConstants.AMP).concat(StringUtil.urlEncode(params));

        String url = INIT_URL + "?" + params + "&sig_sha256=" +
                StringUtil.urlEncode(StringUtil.getHmacSha256Base64(hash, getAccountRoot().getSessionKey()));

        Log.d(Settings.LOG_TAG, url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));

        Log.d(Settings.LOG_TAG, response);

        JSONObject rootObject = new JSONObject(response);
        int status = rootObject.getInt("status");
        switch(status) {
            case 200: {
                JSONObject dataObject = rootObject.getJSONObject("data");
                String host = dataObject.getString("host");
                String urlBody = dataObject.getString("url");

                String uploadUrl = "http://" + host + urlBody;
                String signParams = "a" + "=" + StringUtil.urlEncode(getAccountRoot().getTokenA())
                        + "&" + "aimsid" + "=" + StringUtil.urlEncode(getAccountRoot().getAimSid())
                        + "&" + "client" + "=" + CLIENT_NAME
                        + "&" + "f" + "=" + "json"
                        + "&" + "k" + "=" + StringUtil.urlEncode(IcqSession.DEV_ID_VALUE)
                        + "&" + "ts" + "=" + System.currentTimeMillis() / 1000;
                hash = HttpUtil.POST.concat(WimConstants.AMP).concat(StringUtil.urlEncode(uploadUrl))
                        .concat(WimConstants.AMP).concat(StringUtil.urlEncode(signParams));

                url = uploadUrl + "?" + signParams + "&sig_sha256=" +
                        StringUtil.urlEncode(StringUtil.getHmacSha256Base64(hash, getAccountRoot().getSessionKey()));

                return url;
            }
            default: {
                identifyErrorResponse(status);
            }
        }
        return null;
    }
}
