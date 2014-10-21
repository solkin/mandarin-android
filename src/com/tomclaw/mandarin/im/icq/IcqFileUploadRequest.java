package com.tomclaw.mandarin.im.icq;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.ServerInternalException;
import com.tomclaw.mandarin.core.exceptions.UnauthorizedException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Solkin on 14.10.2014.
 */
public class IcqFileUploadRequest extends NotifiableUploadRequest<IcqAccountRoot> {

    private static final String CLIENT_NAME = "ICQ";
    private final String INIT_URL = "http://files.icq.com/files/init";

    private String buddyId;
    private String cookie;

    private UriFile uriFile;

    public IcqFileUploadRequest() {
    }

    public IcqFileUploadRequest(UriFile uriFile, String buddyId, String cookie) {
        super();
        this.uriFile = uriFile;
        this.buddyId = buddyId;
        this.cookie = cookie;
    }

    @Override
    protected String getDescription() {
        String buddyNick = "";
        Context context = getAccountRoot().getContext();
        try {
            int buddyDbId = QueryHelper.getBuddyDbId(context.getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId);
            buddyNick = QueryHelper.getBuddyNick(context.getContentResolver(), buddyDbId);
        } catch (Throwable ignored) {
        }
        if(TextUtils.isEmpty(buddyNick)) {
            buddyNick = buddyId;
        }
        return context.getString(R.string.file_for, buddyNick);
    }

    @Override
    protected Bitmap getLargeIcon() {
        return uriFile.getThumbnail(getAccountRoot().getContext());
    }

    @Override
    protected void onStartedDelegate() throws Throwable {
        Log.d(Settings.LOG_TAG, "onStarted");
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_RUNNING, cookie);
    }

    @Override
    protected void onSuccessDelegate(String response) throws Throwable {
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
                String text = fileName + " (" + StringUtil.formatBytes(getAccountRoot().getResources(), fileSize) + ")"
                        + "\n" + staticUrl;
                QueryHelper.updateFileStateAndText(getAccountRoot().getContentResolver(),
                        GlobalProvider.HISTORY_CONTENT_STATE_STABLE, text, cookie);
                RequestHelper.requestMessage(getAccountRoot().getContentResolver(),
                        getAccountRoot().getAccountDbId(), buddyId, cookie, staticUrl);
                break;
            }
            default: {
                identifyErrorResponse(status);
            }
        }
    }

    @Override
    protected void onFailDelegate() {
        Log.d(Settings.LOG_TAG, "onFail");
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_FAILED, cookie);
    }

    @Override
    protected void onProgressUpdated(int progress) {
        QueryHelper.updateFileProgress(getAccountRoot().getContentResolver(), progress, cookie);
    }

    @Override
    protected int getProgressStep() {
        return 9;
    }

    @Override
    public VirtualFile getVirtualFile() {
        return uriFile;
    }

    @Override
    protected String getUrl(String name, long size) throws Throwable {
        HttpParamsBuilder builder = new HttpParamsBuilder();
        builder.appendParam("client", CLIENT_NAME);
        builder.appendParam("filename", name);
        builder.appendParam("size", String.valueOf(size));

        String url = getAccountRoot().getSession().signRequest(HttpUtil.GET, INIT_URL, builder);

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
                String body = dataObject.getString("url");

                builder.reset();
                return getAccountRoot().getSession().signRequest(
                        HttpUtil.POST, "http://".concat(host).concat(body), builder);
            }
            default: {
                identifyErrorResponse(status);
            }
        }
        return null;
    }
}
