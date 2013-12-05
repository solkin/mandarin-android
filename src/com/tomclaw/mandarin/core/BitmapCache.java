package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

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

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 8;

    private LruCache<String, Bitmap> bitmapLruCache;

    private final File path;

    public BitmapCache() {
        bitmapLruCache = new LruCache<String, Bitmap>(cacheSize);
        path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        path.mkdirs();
    }

    public Bitmap getBitmapSync(String hash) {
        Bitmap bitmap = bitmapLruCache.get(hash);
        if(bitmap == null) {
            File file = getBitmapFile(hash);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                bitmap = BitmapFactory.decodeStream(inputStream);
                bitmapLruCache.put(hash, bitmap);
            } catch (FileNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while reading file for bitmap hash: " + hash);
            }
        }
        return bitmap;
    }

    public boolean saveBitmapSync(String hash, Bitmap bitmap) {
        File file = getBitmapFile(hash);
        try {
            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, os);
            os.close();
            return true;
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.d(Settings.LOG_TAG, "Error writing bitmap: " + file, e);
        }
        return false;
    }

    private File getBitmapFile(String hash) {
        return new File(path, hash + ".png");
    }

}