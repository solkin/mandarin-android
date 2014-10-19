package com.tomclaw.mandarin.core;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
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
import com.tomclaw.mandarin.util.BitmapHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        if (mimeType.startsWith("image")) {
            return BitmapHelper.decodeSampledBitmapFromUri(context, getUri(), 240, 240);
        } else if (mimeType.startsWith("video")) {
            return BitmapHelper.createVideoThumbnail(context, getUri(), 240);
        }
        return null;
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
