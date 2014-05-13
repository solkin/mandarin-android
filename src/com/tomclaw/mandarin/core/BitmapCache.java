package com.tomclaw.mandarin.core;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/5/13
 * Time: 1:32 AM
 */
public class BitmapCache {

    private static class Holder {

        static BitmapCache instance = new BitmapCache();
    }

    public static BitmapCache getInstance() {
        return Holder.instance;
    }

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    public static final int BITMAP_SIZE_ORIGINAL = 0;
    private static final String BITMAP_CACHE_FOLDER = "bitmaps";
    private File path;
    private LruCache<String, Bitmap> bitmapLruCache;

    public BitmapCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        int cacheSize = maxMemory / 8;
        bitmapLruCache = new LruCache<String, Bitmap>(cacheSize);
    }

    public void initStorage(Context context) {
        path = context.getDir(BITMAP_CACHE_FOLDER, Context.MODE_PRIVATE);
    }

    private static String getCacheKey(String hash, int width, int height) {
        return hash + "_" + width + "_" + height;
    }

    /**
     * Setup required image by hash on specified image view in background thread.
     * While image loading from disk cache and scaling, on image view will be placed default resource.
     *
     * @param imageView       - image view to show image
     * @param hash            - required image hash
     * @param defaultResource - default resource to show while original image being loaded and scaled
     */
    public void getBitmapAsync(ImageView imageView, final String hash, int defaultResource) {
        Bitmap bitmap = getBitmapSyncFromCache(hash, imageView.getWidth(), imageView.getHeight());
        // Checking for there is no cached bitmap and reset is really required.
        if (bitmap == null && BitmapTask.isResetRequired(imageView, hash)) {
            imageView.setImageResource(defaultResource);
        }
        imageView.setTag(hash);
        if (!TextUtils.isEmpty(hash)) {
            // Checking for bitmap cached or not.
            if (bitmap == null) {
                TaskExecutor.getInstance().execute(new BitmapTask(imageView, hash));
            } else {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public Bitmap getBitmapSyncFromCache(String hash, int width, int height) {
        String cacheKey = getCacheKey(hash, width, height);
        return bitmapLruCache.get(cacheKey);
    }

    /**
     * Returns bitmap from memory LRU cache or load image
     * from disk first if there is no image in memory cache.
     * Image of every size will be loaded from disk, scaled and cached!
     *
     * @param hash           - required image hash
     * @param width          - required width
     * @param height         - required height
     * @param isProportional - proportional scale flag
     * @return Bitmap or null if such image not found.
     */
    public Bitmap getBitmapSync(String hash, int width, int height, boolean isProportional) {
        String cacheKey = getCacheKey(hash, width, height);
        Bitmap bitmap = bitmapLruCache.get(cacheKey);
        if (bitmap == null) {
            File file = getBitmapFile(hash);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                bitmap = BitmapFactory.decodeStream(inputStream);
                // Check and set original size.
                if (width == BITMAP_SIZE_ORIGINAL) {
                    width = bitmap.getWidth();
                }
                if (height == BITMAP_SIZE_ORIGINAL) {
                    height = bitmap.getHeight();
                }
                // Resize bitmap for the largest size.
                if (isProportional) {
                    if (bitmap.getWidth() > bitmap.getHeight()) {
                        height = width * bitmap.getHeight() / bitmap.getWidth();
                    } else if (bitmap.getHeight() > bitmap.getWidth()) {
                        width = height * bitmap.getWidth() / bitmap.getHeight();
                    }
                }
                // Check for bitmap needs to be resized.
                if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }
                bitmapLruCache.put(cacheKey, bitmap);
            } catch (FileNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Bitmap '" + hash + "' not found!");
            } catch (Throwable ex) {
                Log.d(Settings.LOG_TAG, "Couldn't cache '" + hash + "' bitmap!", ex);
            }
        }
        return bitmap;
    }

    public boolean saveBitmapSync(String hash, Bitmap bitmap) {
        File file = getBitmapFile(hash);
        try {
            OutputStream os = new FileOutputStream(file);
            bitmap.compress(COMPRESS_FORMAT, 95, os);
            os.close();
            return true;
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.d(Settings.LOG_TAG, "Error writing bitmap: " + file, e);
        }
        return false;
    }

    public void removeBitmap(String hash) {
        File file = getBitmapFile(hash);
        file.delete();
    }

    private File getBitmapFile(String hash) {
        return new File(path, hash.concat(".").concat(COMPRESS_FORMAT.name()));
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * metrics.density);
    }
}