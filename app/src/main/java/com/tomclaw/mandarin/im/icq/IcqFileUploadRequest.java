package com.tomclaw.mandarin.im.icq;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapFile;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.NotifiableUploadRequest;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.UriFile;
import com.tomclaw.mandarin.core.VirtualFile;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Solkin on 14.10.2014.
 */
public class IcqFileUploadRequest extends NotifiableUploadRequest<IcqAccountRoot> {

    private static final String CLIENT_NAME = "ICQ";
    private static final String INIT_URL = "https://files.icq.com/files/init";

    private String buddyId;
    private String cookie;

    private UriFile uriFile;

    private transient VirtualFile virtualFile;

    public IcqFileUploadRequest() {
    }

    public IcqFileUploadRequest(UriFile uriFile, String buddyId, String cookie) {
        super();
        this.uriFile = uriFile;
        this.buddyId = buddyId;
        this.cookie = cookie;
    }

    @Override
    protected PendingIntent getIntent() {
        // Show chat activity with concrete buddy.
        Context context = getAccountRoot().getContext();
        try {
            int buddyDbId = QueryHelper.getBuddyDbId(context.getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId);
            return PendingIntent.getActivity(context, 0,
                    new Intent(context, ChatActivity.class)
                            .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT);
        } catch (Throwable ignored) {
            // No such buddy?!
            // Okay, open chats at least.
            return PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT);
        }
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
        if (TextUtils.isEmpty(buddyNick)) {
            buddyNick = buddyId;
        }
        return context.getString(R.string.file_for, buddyNick);
    }

    @Override
    protected void onStartedDelegate() throws Throwable {
        Logger.log("onStarted");
        try {
            int contentState = QueryHelper.getFileState(getAccountRoot().getContentResolver(),
                    GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
            if (contentState == GlobalProvider.HISTORY_CONTENT_STATE_INTERRUPT ||
                    contentState == GlobalProvider.HISTORY_CONTENT_STATE_STOPPED) {
                throw new InterruptedException();
            }
        } catch (MessageNotFoundException ignored) {
            // Message may be already deleted. So, delete this task too.
            throw new UnknownResponseException();
        }
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_RUNNING, GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
        // Try to compress outgoing file.
        try {
            virtualFile = BitmapFile.create(getAccountRoot().getContext(), uriFile);
            QueryHelper.updateFileSize(getAccountRoot().getContentResolver(), virtualFile.getSize(),
                    GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
        } catch (Throwable ignored) {
            virtualFile = uriFile;
        }
    }

    @Override
    protected void onSuccessDelegate(String response) throws Throwable {
        JSONObject rootObject = new JSONObject(response);
        int status = rootObject.getInt("status");
        switch (status) {
            case 200: {
                JSONObject dataObject = rootObject.getJSONObject("data");
                int previewable = dataObject.getInt("is_previewable");
                String fileId = dataObject.getString("fileid");
                long fileSize = dataObject.getLong("filesize");
                String fileName = dataObject.getString("filename");
                String mime = dataObject.getString("mime");
                String staticUrl = dataObject.getString("static_url");
                Logger.log("onSuccess: " + staticUrl);
                String text = fileName + " (" + StringUtil.formatBytes(getAccountRoot().getResources(), fileSize) + ")"
                        + "\n" + staticUrl;
                QueryHelper.updateFileStateAndText(getAccountRoot().getContentResolver(),
                        GlobalProvider.HISTORY_CONTENT_STATE_STABLE, text,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
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
        Logger.log("onFail");
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_FAILED,
                GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
    }

    @Override
    protected void onCancelDelegate() {
        // Update message to be in waiting state.
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_STOPPED, GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
    }

    @Override
    protected void onPendingDelegate() {

    }

    @Override
    protected void onProgressUpdated(int progress) {
        QueryHelper.updateFileProgress(getAccountRoot().getContentResolver(), progress,
                GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie);
    }

    @Override
    protected long getProgressStepDelay() {
        return 1000;
    }

    @Override
    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    @Override
    protected String getUrl(String name, long size) throws Throwable {
        HttpParamsBuilder builder = new HttpParamsBuilder()
                .appendParam("client", CLIENT_NAME)
                .appendParam("filename", name)
                .appendParam("size", String.valueOf(size));

        String url = getAccountRoot().getSession().signRequest(HttpUtil.GET, INIT_URL, builder);

        Logger.log(url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));

        Logger.log(response);

        JSONObject rootObject = new JSONObject(response);
        int status = rootObject.getInt("status");
        switch (status) {
            case 200: {
                JSONObject dataObject = rootObject.getJSONObject("data");
                String host = dataObject.getString("host");
                String body = dataObject.getString("url");

                builder.reset();
                return getAccountRoot().getSession().signRequest(
                        HttpUtil.POST, "https://".concat(host).concat(body), builder);
            }
            default: {
                identifyErrorResponse(status);
            }
        }
        return null;
    }
}
