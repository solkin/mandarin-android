package com.tomclaw.mandarin.core;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Solkin on 18.10.2014.
 */
public abstract class VirtualFile {

    public VirtualFile() {
    }

    public abstract String getMimeType();

    public abstract long getSize();

    public abstract String getName();

    public abstract InputStream openInputStream(Context context) throws FileNotFoundException;

    public abstract Bitmap getThumbnail(Context context);

    public int getContentType() {
        String mimeType = getMimeType();
        if (mimeType.startsWith("image")) {
            return GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE;
        } else if (mimeType.startsWith("video")) {
            return GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO;
        } else {
            return GlobalProvider.HISTORY_CONTENT_TYPE_FILE;
        }
    }

    public abstract String getPath();
}
