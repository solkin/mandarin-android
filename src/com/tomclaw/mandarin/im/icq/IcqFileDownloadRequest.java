package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.RangedDownloadRequest;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by solkin on 30.10.14.
 */
public class IcqFileDownloadRequest extends NotifiableDownloadRequest<IcqAccountRoot> {

    private final String buddyId;
    private final String cookie;
    private final long time;
    private final String fileId;
    private final String fallbackMessage;

    private transient long fileSize;
    private transient File storeFile;

    public IcqFileDownloadRequest(String buddyId, String cookie, long time, String fileId, String fallbackMessage) {
        this.buddyId = buddyId;
        this.cookie = cookie;
        this.time = time;
        this.fileId = fileId;
        this.fallbackMessage = fallbackMessage;
    }

    @Override
    public String getUrl() throws Throwable {
        String fileInfoUrl = "http://files.icq.net/get/" + fileId + "?json=1&meta=1";
        HttpURLConnection connection = (HttpURLConnection) new URL(fileInfoUrl).openConnection();
        String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));

        Log.d(Settings.LOG_TAG, response);

        JSONObject rootObject = new JSONObject(response);
        int status = rootObject.getInt("status");
        switch (status) {
            case 200: {
                int fileCount = rootObject.getInt("file_count");
                JSONArray fileList = rootObject.getJSONArray("file_list");
                if(fileCount > 0 && fileList.length() > 0) {
                    // No multi-file support. It's really
                    // rare case and a lot of strange logic.
                    JSONObject file = fileList.getJSONObject(0);
                    fileSize = file.getLong("filesize");
                    String fileName = file.getString("filename");
                    String downloadLink = file.getString("dlink");
                    int isPreviewable = file.getInt("is_previewable");
                    String previewUrl = file.optString("static");
                    String mimeType = file.getString("mime");
                    // Downloading preview.
                    String previewHash = "";
                    if(isPreviewable == 1 && !TextUtils.isEmpty(previewUrl)) {
                        Bitmap previewBitmap = getPreviewBitmap(previewUrl);
                        if(previewBitmap != null) {
                            previewHash = HttpUtil.getUrlHash(previewUrl);
                            saveBitmap(previewBitmap, previewHash);
                        }
                    }
                    // Saving obtained data to the history table.
                    storeFile = new File(Environment.getExternalStoragePublicDirectory(getStorageFolder(mimeType)), fileName);
                    int buddyDbId = QueryHelper.getBuddyDbId(getAccountRoot().getContentResolver(),
                            getAccountRoot().getAccountDbId(), buddyId);
                    int contentType = getContentType(mimeType);
                    QueryHelper.insertIncomingFileMessage(getAccountRoot().getContentResolver(), buddyDbId, cookie,
                            storeFile.getPath(), fileName, contentType, fileSize, previewHash);
                    return downloadLink;
                }
            }
            default: {
                // TODO: create fallback message
                throw new DownloadException();
            }
        }
    }

    private Bitmap getPreviewBitmap(String url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        return BitmapFactory.decodeStream(HttpUtil.executeGet(urlConnection));
    }

    private boolean saveBitmap(Bitmap bitmap, String hash) {
        return BitmapCache.getInstance().saveBitmapSync(hash, bitmap);
    }

    private String getStorageFolder(String mimeType) {
        if (mimeType.startsWith("image")) {
            return Environment.DIRECTORY_PICTURES;
        } else if (mimeType.startsWith("video")) {
            return Environment.DIRECTORY_MOVIES;
        }  else if (mimeType.startsWith("audio")) {
            return Environment.DIRECTORY_MUSIC;
        } else {
            return Environment.DIRECTORY_DOWNLOADS;
        }
    }

    public int getContentType(String mimeType) {
        if (mimeType.startsWith("image")) {
            return GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE;
        } else if (mimeType.startsWith("video")) {
            return GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO;
        } else {
            return GlobalProvider.HISTORY_CONTENT_TYPE_FILE;
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public FileOutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(storeFile);
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
        return context.getString(R.string.file_from, buddyNick);
    }

    @Override
    protected void onStartedDelegate() {
        Log.d(Settings.LOG_TAG, "onStarted");
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_RUNNING, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected long getProgressStepDelay() {
        return 1000;
    }

    @Override
    protected void onProgressUpdated(int progress) {
        QueryHelper.updateFileProgress(getAccountRoot().getContentResolver(), progress,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected void onSuccessDelegate() {
        QueryHelper.updateFileStateAndText(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_STABLE, fallbackMessage,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected void onFailDelegate() {
        Log.d(Settings.LOG_TAG, "onFail");
        // TODO: update failed message, return fallback
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_FAILED,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }
}
