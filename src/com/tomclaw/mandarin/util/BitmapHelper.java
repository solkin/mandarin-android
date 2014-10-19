package com.tomclaw.mandarin.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;

import java.io.*;

/**
 * Created by Solkin on 17.10.2014.
 */
public class BitmapHelper {

    public static final long KB_IN_BYTES = 1024;
    /**
     * Buffer is large enough to rewind past any EXIF headers.
     */
    private static final int THUMBNAIL_BUFFER_SIZE = (int) (128 * KB_IN_BYTES);

    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        Bitmap bitmap;
        try {
            final Bundle openOpts = new Bundle();
            AssetFileDescriptor afd = context.getContentResolver().openTypedAssetFileDescriptor(uri, "image/*",
                    openOpts);
            final FileDescriptor fd = afd.getFileDescriptor();
            InputStream inputStream = new BufferedInputStream(new FileInputStream(fd), THUMBNAIL_BUFFER_SIZE);
            inputStream.mark(THUMBNAIL_BUFFER_SIZE);

            // MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Calculate inSampleSize
            final int widthSample = options.outWidth / reqWidth;
            final int heightSample = options.outHeight / reqHeight;

            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.min(widthSample, heightSample);

            // Decode bitmap with inSampleSize set
            inputStream.reset();
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        } catch (Throwable ignored) {
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * Create a video thumbnail for a video. May return null if the video is
     * corrupt or the format is not supported.
     *
     * @param context application context
     * @param uri the uri of video file
     */
    public static Bitmap createVideoThumbnail(Context context, Uri uri, int size) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        if (bitmap == null) return null;

        // Scale down the bitmap if it's too large.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int max = Math.max(width, height);
        if (max > size) {
            float scale = ((float) size) / max;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
