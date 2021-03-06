package com.tomclaw.mandarin.im.icq;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.tomclaw.helpers.Files;
import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.NotifiableDownloadRequest;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.DownloadCancelledException;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import static com.tomclaw.mandarin.util.PermissionsHelper.hasPermissions;

/**
 * Created by solkin on 30.10.14.
 */
public class IcqFileDownloadRequest extends NotifiableDownloadRequest<IcqAccountRoot> {

    private static final transient int MAX_META_TRY_COUNT = 5;

    private final String buddyId;
    private final String cookie;
    private final long time;
    private final String fileId;
    private final String fileUrl;
    private final String originalMessage;
    private String tag;
    private boolean isFirstAttempt;
    private String previewHash = "";

    private String cachedFileName;

    private transient long fileSize;
    private transient File storeFile;

    private transient int metaTryCount;

    public IcqFileDownloadRequest(String buddyId, String cookie, long time, String fileId,
                                  String fileUrl, String originalMessage, String tag) {
        this.buddyId = buddyId;
        this.cookie = cookie;
        this.time = time;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.originalMessage = originalMessage;
        this.tag = tag;
        this.isFirstAttempt = true;
    }

    @Override
    public String getUrl() throws Throwable {
        DatabaseLayer databaseLayer = getDatabaseLayer();

        String fileInfoUrl = "https://files.icq.net/get/" + fileId + "?json=1&meta=1";
        HttpURLConnection connection = (HttpURLConnection) new URL(fileInfoUrl).openConnection();
        String response = HttpUtil.streamToString(HttpUtil.executeGet(connection));

        Logger.log(response);

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
            String previewUrl = file.optString("static600");
            String mimeType = file.getString("mime");
            // Checking for download link is empty because file is not ready yet.
            if (TextUtils.isEmpty(downloadLink)) {
                Thread.sleep(1000);
                return getUrl();
            }
            // Downloading preview.
            if (isPreviewable == 1) {
                // Check for preview is ready now or try again after short delay.
                if (TextUtils.isEmpty(previewUrl)) {
                    if (metaTryCount < MAX_META_TRY_COUNT) {
                        // No preview this time. Sleep a while and try again.
                        metaTryCount++;
                        Thread.sleep(1000);
                        return getUrl();
                    }
                } else {
                    // For the first attempt we must download and store preview.
                    if (isFirstAttempt) {
                        // Preview Url is ready - let's download it and cache.
                        Bitmap previewBitmap = getPreviewBitmap(previewUrl);
                        if (previewBitmap != null) {
                            previewHash = HttpUtil.getUrlHash(previewUrl);
                            saveBitmap(previewBitmap, previewHash);
                        }
                    }
                }
            }
            // Saving obtained data to the history table.
            if (TextUtils.isEmpty(cachedFileName)) {
                // This is probable first attempt to download file.
                // Create new file.
                storeFile = getUniqueFile(mimeType, fileName);
                cachedFileName = storeFile.getName();
            } else {
                // Continue downloading to existing file.
                storeFile = new File(getStoragePublicFolder(mimeType), cachedFileName);
            }
            // Make directories for this path.
            storeFile.getParentFile().mkdirs();
            int buddyDbId = QueryHelper.getBuddyDbId(databaseLayer, getAccountRoot().getAccountDbId(), buddyId);
            int contentType = getContentType(mimeType);
            Uri uri = Uri.fromFile(storeFile);
//            TODO: reimplement this.
//            QueryHelper.insertIncomingFileMessage(getAccountRoot().getContentResolver(), buddyDbId, cookie,
//                    time, getUrlMessage(), uri, fileName, contentType, fileSize, previewHash, tag);
            // Check to download file now.
            if (!isStartDownload(isFirstAttempt, fileSize) || !hasStoragePermissions()) {
                // All other attempts will be manual.
                isFirstAttempt = false;
                throw new DownloadCancelledException();
            }
            return downloadLink;
        } else {
//            QueryHelper.insertMessage(getAccountRoot().getContentResolver(),
//                    getAccountRoot().getAccountDbId(), buddyId,
//                    GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie, time, originalMessage);
            throw new DownloadException();
        }
    }

    private File getUniqueFile(String mimeType, String fileName) {
        final String base = Files.getFileBaseFromName(fileName);
        final String extension = Files.getFileExtensionFromPath(fileName);
        File directory = getStoragePublicFolder(mimeType);
        if (!hasStoragePermissions()) {
            String randomName = Strings.generateRandomWord();
            fileName = base + "-" + randomName + "." + extension;
        } else if (directory.exists()) {
            File[] files = directory.listFiles(new FilenameFilter() {
                public boolean accept(File file, String name) {
                    return Files.getFileBaseFromName(name).toLowerCase().startsWith(base.toLowerCase()) &&
                            Files.getFileExtensionFromPath(name).toLowerCase().equals(extension.toLowerCase());
                }
            });
            if (files.length > 0) {
                fileName = base + "-" + files.length + "." + extension;
            }
        }
        return new File(directory, fileName);
    }

    private File getStoragePublicFolder(String mimeType) {
        return Environment.getExternalStoragePublicDirectory(getStorageFolder(mimeType));
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
    public FileOutputStream getOutputStream() throws IOException, DownloadCancelledException {
        if (hasStoragePermissions()) {
            return new FileOutputStream(storeFile);
        }
        throw new DownloadCancelledException();
    }

    private boolean hasStoragePermissions() {
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        return hasPermissions(getService(), PERMISSION);
    }

    @Override
    protected PendingIntent getIntent() {
        // Show chat activity with concrete buddy.
        Context context = getAccountRoot().getContext();
        try {
            int accountDbId = getAccountRoot().getAccountDbId();
            Buddy buddy = new Buddy(accountDbId, buddyId);
            return PendingIntent.getActivity(context, 0,
                    new Intent(context, ChatActivity.class)
                            .putExtra(Buddy.KEY_STRUCT, buddy)
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
        DatabaseLayer databaseLayer = getDatabaseLayer();
        int accountDbId = getAccountRoot().getAccountDbId();
        String buddyNick = "";
        Context context = getAccountRoot().getContext();
        try {
            int buddyDbId = QueryHelper.getBuddyDbId(databaseLayer, accountDbId, buddyId);
            buddyNick = QueryHelper.getBuddyNick(databaseLayer, buddyDbId);
        } catch (Throwable ignored) {
        }
        if (TextUtils.isEmpty(buddyNick)) {
            buddyNick = buddyId;
        }
        return context.getString(R.string.file_from, buddyNick);
    }

    @Override
    protected void onStartedDelegate() throws DownloadCancelledException, DownloadException {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        try {
            int contentState = QueryHelper.getFileState(databaseLayer,
                    GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
            if (contentState == GlobalProvider.HISTORY_CONTENT_STATE_INTERRUPT) {
                throw new DownloadCancelledException();
            }
        } catch (MessageNotFoundException ignored) {
            // This may be if this is first request call and no message inserted yet.
        }
    }

    @Override
    protected void onDownload() {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        QueryHelper.updateFileState(databaseLayer, GlobalProvider.HISTORY_CONTENT_STATE_RUNNING,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected long getProgressStepDelay() {
        return 1000;
    }

    @Override
    protected void onProgressUpdated(int progress) {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        QueryHelper.updateFileProgress(databaseLayer, progress,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected void onSuccessDelegate() {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        QueryHelper.updateFileStateAndText(databaseLayer,
                GlobalProvider.HISTORY_CONTENT_STATE_STABLE, getUrlMessage(),
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
        // Update file in system gallery.
        MediaScannerConnection.scanFile(getAccountRoot().getContext(), new String[]{storeFile.getPath()}, null, null);
    }

    @Override
    protected void onFailDelegate() {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        QueryHelper.revertFileToMessage(databaseLayer,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, originalMessage, cookie);
        // Checking for file is being created.
        if (storeFile != null) {
            // Remove file fragment.
            storeFile.delete();
        }
    }

    @Override
    protected void onCancelDelegate() {
        DatabaseLayer databaseLayer = getDatabaseLayer();
        // Update message to be in waiting state.
        QueryHelper.updateFileState(databaseLayer, GlobalProvider.HISTORY_CONTENT_STATE_STOPPED,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected void onPendingDelegate() {
    }

    private String getUrlMessage() {
        return storeFile.getName() + " (" + Strings.formatBytes(getAccountRoot().getResources(), fileSize) + ")"
                + "\n" + fileUrl;
    }
}
