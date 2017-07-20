package com.tomclaw.mandarin.core;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.manager.RequestManagerRetriever;

/**
 * Created by ivsolkin on 10/07/2017.
 */
public class GlideProvider {

    private static final int DISK_CACHE_SIZE = 128 * 1024 * 1024;
    private static final String DISK_CACHE_DIR = "image_manager_disk_cache";

    private static class Holder {

        static GlideProvider instance = new GlideProvider();
    }

    public static GlideProvider getInstance() {
        return Holder.instance;
    }

    public Glide glide;

    public void init(Application context) {
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context).build();
        DiskCache.Factory diskCacheFactory = new InternalCacheDiskCacheFactory(context,
                DISK_CACHE_DIR, DISK_CACHE_SIZE);
        glide = new GlideBuilder()
                .setDiskCache(diskCacheFactory)
                .setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()))
                .setArrayPool(new LruArrayPool(calculator.getArrayPoolSizeInBytes()))
                .setBitmapPool(new LruBitmapPool(calculator.getBitmapPoolSize()))
                .build(context);
    }

    public Glide getGlide() {
        return glide;
    }

    public static Glide glide() {
        return getInstance().getGlide();
    }

    public static RequestManagerRetriever retriever() {
        return getInstance().getGlide().getRequestManagerRetriever();
    }
}
