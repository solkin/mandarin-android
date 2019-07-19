package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.tomclaw.helpers.Files;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Solkin on 18.10.2014.
 */
public class UriFile extends VirtualFile {

    private String uri;
    private String mimeType;
    private long size;
    private String name;

    public UriFile() {
    }

    private UriFile(String uri, String mimeType, long size, String name) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.size = size;
        this.name = name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getName() {
        return name;
    }

    public Uri getUri() {
        return Uri.parse(uri);
    }

    @Override
    public InputStream openInputStream(Context context) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(getUri());
    }

    @Override
    public Bitmap getThumbnail(Context context) {
        int sizePx = context.getResources().getDimensionPixelSize(R.dimen.preview_bitmap);
        long time = System.currentTimeMillis();
        Bitmap bitmap;
        if (mimeType.startsWith("image")) {
            bitmap = BitmapHelper.decodeSampledBitmapFromUri(context, getUri(), sizePx, sizePx);
        } else if (mimeType.startsWith("video")) {
            bitmap = BitmapHelper.createVideoThumbnail(context, getUri(), sizePx);
        } else {
            bitmap = null;
        }
        Logger.log("preview sampling (" + sizePx + "): " + (System.currentTimeMillis() - time));
        return bitmap;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    @Override
    public String getPath() {
        return uri;
    }

    public static UriFile create(Context context, Uri uri) throws FileNotFoundException {
        String uriScheme = uri.getScheme();
        if (TextUtils.equals(uriScheme, ContentResolver.SCHEME_CONTENT)) {
            String[] projection = {
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    // Size detection.
                    long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                    // Try to detect name.
                    String name;
                    int nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameColumn == -1) {
                        name = String.valueOf(System.currentTimeMillis());
                    } else {
                        name = cursor.getString(nameColumn);
                    }
                    // Try to detect mime-type.
                    String mimeType;
                    int mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                    if (mimeTypeColumn == -1) {
                        mimeType = Files.getMimeType(name);
                    } else {
                        mimeType = cursor.getString(mimeTypeColumn);
                    }
                    return new UriFile(uri.toString(), mimeType, size, name);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (TextUtils.equals(uriScheme, ContentResolver.SCHEME_FILE)) {
            File file = new File(uri.getPath());
            return new UriFile(uri.toString(), Files.getMimeType(file.getName()), file.length(), file.getName());
        }
        throw new FileNotFoundException();
    }

    @Override
    public String toString() {
        return "UriFile{" +
                "uri='" + uri + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + size +
                ", name='" + name + '\'' +
                '}';
    }
}
