package com.tomclaw.mandarin.core;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.BitmapHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

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
        float sizeDp = context.getResources().getDimension(R.dimen.preview_bitmap);
        int sizePx = (int) convertDpToPixel(sizeDp, context);
        long time = System.currentTimeMillis();
        Bitmap bitmap;
        if (mimeType.startsWith("image")) {
            bitmap = BitmapHelper.decodeSampledBitmapFromUri(context, getUri(), sizePx, sizePx);
        } else if (mimeType.startsWith("video")) {
            bitmap = BitmapHelper.createVideoThumbnail(context, getUri(), sizePx);
        } else {
            bitmap = null;
        }
        Log.d(Settings.LOG_TAG, "preview sampling (" + sizePx + "): " + (System.currentTimeMillis() - time));
        return bitmap;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    @Override
    public String getPath() {
        return uri;
    }

    public static UriFile create(Context context, Uri uri) throws FileNotFoundException {
        String[] projection = {
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = null;
        try {
            cursor = context.getApplicationContext().getContentResolver().query(uri, projection, null, null, null);
            if(cursor.moveToFirst()) {
                return new UriFile(uri.toString(),
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
