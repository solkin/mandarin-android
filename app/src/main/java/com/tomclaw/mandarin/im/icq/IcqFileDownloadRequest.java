package com.tomclaw.mandarin.im.icq;

import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO;
import static com.tomclaw.mandarin.util.PermissionsHelper.hasPermissions;
import static com.tomclaw.mandarin.util.StringUtil.generateRandomWord;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.NotifiableDownloadRequest;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.DownloadCancelledException;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * Created by solkin on 30.10.14.
 */
public class IcqFileDownloadRequest extends NotifiableDownloadRequest<IcqAccountRoot> {

    private static final int MAX_META_TRY_COUNT = 5;

    private final String buddyId;
    private final String cookie;
    private final long time;
    private final String fileId;
    private final String fileUrl;
    private final String originalMessage;
    private final String tag;
    private boolean isFirstAttempt;
    private String previewHash = "";

    private String cachedFileName;

    private transient long fileSize;
    private transient Uri storeUri;

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


            int contentType = getContentType(mimeType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri collections;
                switch (contentType) {
                    case HISTORY_CONTENT_TYPE_PICTURE: {
                        collections = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        break;
                    }
                    case HISTORY_CONTENT_TYPE_VIDEO: {
                        collections = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        break;
                    }
                    default: {
                        collections = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        break;
                    }
                }

                ContentValues fileDetails = new ContentValues();
                fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                fileDetails.put(MediaStore.Images.Media.MIME_TYPE, mimeType);

                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    fileDetails.put(MediaStore.Images.Media.IS_PENDING, 1);
                }*/

                ContentResolver resolver = getAccountRoot().getContext().getContentResolver();
                if (resolver == null) return null;
                storeUri = resolver.insert(collections, fileDetails);
                if (storeUri == null) throw new IllegalArgumentException();
            } else {
                File storeFile;
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
                storeUri = Uri.fromFile(storeFile);
            }

            int buddyDbId = QueryHelper.getBuddyDbId(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId);

            QueryHelper.insertIncomingFileMessage(getAccountRoot().getContentResolver(), buddyDbId, cookie,
                    time, getUrlMessage(), storeUri, fileName, contentType, fileSize, previewHash, tag);
            // Check to download file now.
            if (!isStartDownload(isFirstAttempt, fileSize) || !hasStoragePermissions()) {
                // All other attempts will be manual.
                isFirstAttempt = false;
                throw new DownloadCancelledException();
            }
            return downloadLink;
        } else {
            QueryHelper.insertMessage(getAccountRoot().getContentResolver(),
                    PreferenceHelper.isCollapseMessages(getAccountRoot().getContext()),
                    getAccountRoot().getAccountDbId(), buddyId, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING,
                    GlobalProvider.HISTORY_MESSAGE_STATE_UNDETERMINED, cookie, time, originalMessage);
            throw new DownloadException();
        }
    }

    /*private Uri saveFileToMediaStore(Context context, String displayName, String mimeType) {
        Uri collections;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collections = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            collections = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        ContentValues fileDetails = new ContentValues();
        fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        fileDetails.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileDetails.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        if (resolver == null) return null;
        return resolver.insert(collections, fileDetails);

        try {
            OutputStream os = resolver.openOutputStream(imageContentUri, "w");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear();
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(imageContentUri, imageDetails, null, null);
            }

            return imageContentUri;
        } catch (FileNotFoundException e) {
            // Some legacy devices won't create directory for the Uri if dir not exist, resulting in
            // a FileNotFoundException. To resolve this issue, we should use the File API to save the
            // image, which allows us to create the directory ourselves.
            return null;
        }
    }*/

    private File getUniqueFile(String mimeType, String fileName) {
        final String base = FileHelper.getFileBaseFromName(fileName);
        final String extension = FileHelper.getFileExtensionFromPath(fileName);
        File directory = getStoragePublicFolder(mimeType);
        if (!hasStoragePermissions()) {
            Random random = new Random(System.currentTimeMillis());
            String randomName = generateRandomWord(random);
            fileName = base + "-" + randomName + "." + extension;
        } else if (directory.exists()) {
            File[] files = directory.listFiles((file, name) -> FileHelper.getFileBaseFromName(name).toLowerCase().startsWith(base.toLowerCase()) &&
                    FileHelper.getFileExtensionFromPath(name).equalsIgnoreCase(extension));
            if (files != null && files.length > 0) {
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
            return HISTORY_CONTENT_TYPE_PICTURE;
        } else if (mimeType.startsWith("video")) {
            return HISTORY_CONTENT_TYPE_VIDEO;
        } else {
            return GlobalProvider.HISTORY_CONTENT_TYPE_FILE;
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public OutputStream getOutputStream() throws IOException, DownloadCancelledException {
        if (!hasStoragePermissions()) {
            throw new DownloadCancelledException();
        }
        return getAccountRoot().getContext().getContentResolver().openOutputStream(storeUri, "w");
    }

    private boolean hasStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            return hasPermissions(getService(), PERMISSION);
        }
        return true;
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
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } catch (Throwable ignored) {
            // No such buddy?!
            // Okay, open chats at least.
            return PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
        return context.getString(R.string.file_from, buddyNick);
    }

    @Override
    protected void onStartedDelegate() throws DownloadCancelledException, DownloadException {
        try {
            int contentState = QueryHelper.getFileState(getAccountRoot().getContentResolver(),
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
        MediaScannerConnection.scanFile(getAccountRoot().getContext(), new String[]{storeUri.getPath()}, null, null);
    }

    @Override
    protected void onFailDelegate() {
        QueryHelper.revertFileToMessage(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, originalMessage, cookie);
        // Checking for file is being created.
        if (storeUri != null) {
            // Remove file fragment.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getAccountRoot().getContext().getContentResolver().delete(storeUri, null);
            }
        }
    }

    @Override
    protected void onCancelDelegate() {
        // Update message to be in waiting state.
        QueryHelper.updateFileState(getAccountRoot().getContentResolver(),
                GlobalProvider.HISTORY_CONTENT_STATE_STOPPED, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie);
    }

    @Override
    protected void onPendingDelegate() {
    }

    private String getUrlMessage() {
        return storeUri.getLastPathSegment() + " (" + StringUtil.formatBytes(getAccountRoot().getResources(), fileSize) + ")"
                + "\n" + fileUrl;
    }
}
