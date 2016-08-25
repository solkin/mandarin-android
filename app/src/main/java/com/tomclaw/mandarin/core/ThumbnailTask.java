package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.tomclaw.mandarin.main.views.LazyImageView;

/**
 * Created by Solkin on 05.11.2014.
 */
public class ThumbnailTask extends WeakObjectTask<LazyImageView> {

    private Bitmap bitmap;
    private final String hash;
    private long imageId;
    private int width, height;

    public ThumbnailTask(LazyImageView imageView, String hash, long imageId) {
        this(imageView, hash, imageId, imageView.getWidth(), imageView.getHeight());
    }

    public ThumbnailTask(LazyImageView imageView, String hash, long imageId, int width, int height) {
        super(imageView);
        this.hash = hash;
        this.imageId = imageId;
        this.width = width;
        this.height = height;
    }

    public static String getImageTag(LazyImageView imageView) {
        return imageView.getHash();
    }

    public static boolean isResetRequired(LazyImageView imageView, String hash) {
        return !TextUtils.equals(imageView.getHash(), hash);
    }

    @Override
    public void executeBackground() throws Throwable {
        LazyImageView image = getWeakObject();
        if (image != null) {
            bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true, false);
            if (bitmap == null) {
                int thumbnailKind = BitmapCache.getInstance().isLowDensity() ?
                        MediaStore.Images.Thumbnails.MICRO_KIND : MediaStore.Images.Thumbnails.MINI_KIND;
                Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                        image.getContext().getContentResolver(), imageId, thumbnailKind, null);
                if (thumbnail != null) {
                    BitmapCache.getInstance().saveBitmapSync(hash, thumbnail, Bitmap.CompressFormat.JPEG);
                    thumbnail.recycle();
                    bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true, false);
                }
            }
        }
    }

    @Override
    public void onSuccessMain() {
        LazyImageView image = getWeakObject();
        // Hash may be changed in another task.
        if (image != null && bitmap != null && TextUtils.equals(image.getHash(), hash)) {
            image.setBitmap(bitmap);
        }
    }
}
