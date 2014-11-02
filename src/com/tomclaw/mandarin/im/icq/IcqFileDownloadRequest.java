package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.DownloadCancelledException;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by solkin on 30.10.14.
 */
public class IcqFileDownloadRequest extends NotifiableDownloadRequest<IcqAccountRoot> {

    private final String buddyId;
    private final String cookie;
    private final long time;
    private final String fileId;
    private final String fileUrl;
    private final String originalMessage;

    private transient long fileSize;
    private transient File storeFile;

    public IcqFileDownloadRequest(String buddyId, String cookie, long time,
                                  String fileId, String fileUrl, String originalMessage) {
        this.buddyId = buddyId;
        this.cookie = cookie;
        this.time = time;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.originalMessage = originalMessage;
    }

    @Override
    public String getUrl() throws Throwable {
        String fileInfoUrl = "http://files.icq.net/get/" + fileId + "?json=1&meta=1";
        HttpURLConnection connection = (HttpURLConnection) new URL(fileInfoUrl).openConnection();
        String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));

        Log.d(Settings.LOG_TAG, response);

        JSONObject rootObject = new JSONObject(response);
        int fileCount = rootObject.getInt("file_count");
        JSONArray fileList = rootObject.getJSONArray("file_list");
        if (fileCount > 0 && fileList.length() > 0) {
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
            if (isPreviewable == 1 && !TextUtils.isEmpty(previewUrl)) {
                Bitmap previewBitmap = getPreviewBitmap(previewUrl);
                if (previewBitmap != null) {
                    previewHash = HttpUtil.getUrlHash(previewUrl);
                    saveBitmap(previewBitmap, previewHash);
                }
            }
            // Saving obtained data to the history table.
            storeFile = new File(
                    Environment.getExternalStoragePublicDirectory(getStorageFolder(mimeType)), fileName);
            int buddyDbId = QueryHelper.getBuddyDbId(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId);
            int contentType = getContentType(mimeType);
            QueryHelper.insertIncomingFileMessage(getAccountRoot().getContentResolver(), buddyDbId, cookie,
                    time, getUrlMessage(), storeFile.getPath(), fileName, contentType, fileSize, previewHash);
            // Check to download file now.
            if (!isStartDownload()) {
                throw new DownloadCancelledException();
            }
            return downloadLink;
        } else {
            QueryHelper.insertMessage(getAccountRoot().getContentResolver(),
                    PreferenceHelper.isCollapseMessages(getAccountRoot().getContext()),
                    getAccountRoot().getAccountDbId(), buddyId, 1, 2, cookie, time, originalMessage, true);
            throw new DownloadException();
        }
    }

    private boolean isStartDownload() {
        // TODO: check specified constructor flag and preferences.
        return true;
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
        } else if (mimeType.startsWith("audio")) {
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
    }

    @Override
    protected void onDownload() {
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
                GlobalProvider.HISTORY_CONTENT_STATE_STABLE, getUrlMessage(),
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
        // Update file in system gallery.
        MediaScannerConnection.scanFile(getAccountRoot().getContext(), new String[]{storeFile.getPath()}, null, null);
    }

    @Override
    protected void onFailDelegate() {
        QueryHelper.revertFileToMessage(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, originalMessage, cookie);
        // Remove file fragment.
        storeFile.delete();
    }

    @Override
    protected void onCancelDelegate() {
        // Update message to be in waiting state.
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_WAITING, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    private String getUrlMessage() {
        return storeFile.getName() + " (" + StringUtil.formatBytes(getAccountRoot().getResources(), fileSize) + ")"
                + "\n" + fileUrl;
    }
}
