package com.tomclaw.mandarin.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            bitmap = decodeSampledBitmapFromStream(inputStream, reqWidth, reqHeight);

            int orientation = getOrientation(context, uri);
            if (orientation != 0) {
                final int width = bitmap.getWidth();
                final int height = bitmap.getHeight();

                final Matrix m = new Matrix();
                m.setRotate(orientation, width / 2, height / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
            }
        } catch (Throwable ignored) {
            bitmap = null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }

    public static Bitmap decodeSampledBitmapFromStream(InputStream stream, int reqWidth, int reqHeight) {
        Bitmap bitmap;
        try {
            InputStream inputStream = new BufferedInputStream(stream, THUMBNAIL_BUFFER_SIZE);
            inputStream.mark(THUMBNAIL_BUFFER_SIZE);

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Calculate inSampleSize
            final float widthSample = options.outWidth / reqWidth;
            final float heightSample = options.outHeight / reqHeight;

            float scaleFactor = Math.max(widthSample, heightSample);
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = (int) scaleFactor;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

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
     * @param uri     the uri of video file
     */
    public static Bitmap createVideoThumbnail(Context context, Uri uri, int size) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            bitmap = retriever.getFrameAtTime(-1);
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
        int min = Math.min(width, height);
        if (min > size) {
            float scale = ((float) size) / min;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
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

    public static int getOrientation(Context context, Uri uri) {
        final String scheme = uri.getScheme();
        final String authority = uri.getAuthority();
        int rotation = 0;
        String path;
        if (TextUtils.equals(uri.getScheme(), ContentResolver.SCHEME_CONTENT)) {
            path = getMediaPath(context, uri);
        } else {
            path = uri.getPath();
        }
        if (!TextUtils.isEmpty(path)) {
            int exifOrientation = obtainFileOrientation(path);
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    rotation = 270;
                    break;
                default:
                    rotation = 0;
                    break;
            }
            if (exifOrientation != ExifInterface.ORIENTATION_UNDEFINED) {
                return rotation;
            }
        }
        // No file access, let's check in media store.
        if (ContentResolver.SCHEME_CONTENT.equals(scheme) && MediaStore.AUTHORITY.equals(authority) && context != null) {
            final String[] projection = new String[]{MediaStore.Images.Media.ORIENTATION};
            final Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                final int orientationColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
                if (cursor.moveToFirst()) {
                    rotation = cursor.isNull(orientationColumnIndex) ? 0 : cursor.getInt(orientationColumnIndex);
                }
                cursor.close();
            }
        }
        return rotation;
    }

    private static int obtainFileOrientation(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
        try {
            ExifInterface exifInterface = new ExifInterface(fileName);
            return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }

    public static String getMediaPath(Context context, Uri uri) {
        String path = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int dataColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                path = cursor.getString(dataColumnIndex);
            }
        } catch (Throwable ignore) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    public static Bitmap getRoundBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}
