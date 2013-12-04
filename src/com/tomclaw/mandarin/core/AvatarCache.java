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
public class AvatarCache {

    private static class Holder {

        static AvatarCache instance = new AvatarCache();
    }

    public static AvatarCache getInstance() {
        return Holder.instance;
    }

    private static final int cacheSize = 4 * 1024 * 1024; // 4MiB

    private LruCache<String, Bitmap> bitmapLruCache;

    private final File path;

    public AvatarCache() {
        bitmapLruCache = new LruCache<String, Bitmap>(cacheSize);
        path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        path.mkdirs();
    }

    public Bitmap getBitmapSync(String avatarHash) {
        Bitmap bitmap = bitmapLruCache.get(avatarHash);
        if(bitmap == null) {
            File file = getBitmapFile(avatarHash);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                bitmap = BitmapFactory.decodeStream(inputStream);
                bitmapLruCache.put(avatarHash, bitmap);
            } catch (FileNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while reading file for avatar hash: " + avatarHash);
            }
        }
        return bitmap;
    }

    public boolean saveBitmapSync(String avatarHash, Bitmap bitmap) {
        File file = getBitmapFile(avatarHash);
        try {
            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, os);
            os.close();
            return true;
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.d(Settings.LOG_TAG, "Error writing avatar: " + file, e);
        }
        return false;
    }

    private File getBitmapFile(String avatarHash) {
        return new File(path, avatarHash + ".png");
    }

}