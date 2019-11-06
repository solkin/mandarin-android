package com.tomclaw.mandarin.core;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public final class MandarinGlideModule extends AppGlideModule {

    private static final int DISK_CACHE_SIZE = 128 * 1024 * 1024;
    private static final String DISK_CACHE_DIR = "image_manager_disk_cache";

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context).build();
        DiskCache.Factory diskCacheFactory = new InternalCacheDiskCacheFactory(context,
                DISK_CACHE_DIR, DISK_CACHE_SIZE);
        builder
                .setDiskCache(diskCacheFactory)
                .setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()))
                .setArrayPool(new LruArrayPool(calculator.getArrayPoolSizeInBytes()))
                .setBitmapPool(new LruBitmapPool(calculator.getBitmapPoolSize()));
    }

}
