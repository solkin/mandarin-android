package com.tomclaw.mandarin.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

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
            throws UnsupportedFileTypeException, ImageCompressionDesireLackException {
        // Check for this is image and not GIF.
        String extension = FileHelper.getFileExtensionFromPath(file.getName());
        if (file.getMimeType().startsWith("image") &&
                !TextUtils.equals(extension.toLowerCase(), "gif")) {
            // Check for preferences of image compression.
            String imageCompression = PreferenceHelper.getImageCompression(context);
            if (TextUtils.equals(imageCompression, context.getString(R.string.compression_original))) {
                throw new ImageCompressionDesireLackException();
            }
            int sampleSize;
            int quality;
            if (TextUtils.equals(imageCompression, context.getString(R.string.compression_medium))) {
                sampleSize = 768;
                quality = 75;
            } else {
                sampleSize = 480;
                quality = 60;
            }
            // Now we can compress this image with pleasure.
            Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromUri(context, file.getUri(), sampleSize, sampleSize);
            if (bitmap != null) {
                File bitmapFile = saveBitmapSync(file.getName(), bitmap, quality);
                if (bitmapFile != null) {
                    return new BitmapFile(bitmapFile);
                }
            }
        }
        throw new UnsupportedFileTypeException();
    }

    public static File saveBitmapSync(String fileName, Bitmap bitmap, int quality) {
        try {
            File file = File.createTempFile("MND_",
                    "." + FileHelper.getFileExtensionFromPath(fileName));
            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
            os.flush();
            os.close();
            return file;
        } catch (Throwable ex) {
            // Unable to create file, likely because external storage is
            // not currently mounted or OutOfMemory was thrown.
            Logger.log("Error saving bitmap: " + fileName, ex);
        }
        return null;
    }

    public static class UnsupportedFileTypeException extends Throwable {
    }

    public static class ImageCompressionDesireLackException extends Throwable {
    }
}
