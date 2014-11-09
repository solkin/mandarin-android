package com.tomclaw.mandarin.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.HttpUtil;

import java.io.*;

/**
 * Created by Solkin on 03.11.2014.
 */
public class BitmapFile extends VirtualFile {

    private File file;

    private BitmapFile(File file) {
        this.file = file;
    }

    @Override
    public String getMimeType() {
        return "image/jpeg";
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream openInputStream(Context context) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public Bitmap getThumbnail(Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public static BitmapFile create(Context context, UriFile file)
            throws UnsupportedFileTypeException {
        if(file.getMimeType().startsWith("image")) {
            // Now we can compress this image with pleasure.
            Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromUri(context, file.getUri(), 768, 768);
            if(bitmap != null) {
                File bitmapFile = saveBitmapSync(file.getName(), bitmap);
                if (bitmapFile != null) {
                    return new BitmapFile(bitmapFile);
                }
            }
        }
        throw new UnsupportedFileTypeException();
    }

    public static File saveBitmapSync(String fileName, Bitmap bitmap) {
        try {
            File file = File.createTempFile(
                    HttpUtil.getFileBaseFromName(fileName),
                    HttpUtil.getFileExtensionFromPath(fileName));
            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);
            os.flush();
            os.close();
            return file;
        } catch (Throwable ex) {
            // Unable to create file, likely because external storage is
            // not currently mounted or OutOfMemory was thrown.
            Log.d(Settings.LOG_TAG, "Error saving bitmap: " + fileName, ex);
        }
        return null;
    }

    public static class UnsupportedFileTypeException extends Throwable {
    }
}
