package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * Created by Solkin on 05.11.2014.
 */
public class ThumbnailTask extends WeakObjectTask<ImageView> {

    private Bitmap bitmap;
    private final String hash;
    private long imageId;
    private int width, height;

    public ThumbnailTask(ImageView imageView, String hash, long imageId) {
        this(imageView, hash, imageId, imageView.getWidth(), imageView.getHeight());
    }

    public ThumbnailTask(ImageView imageView, String hash, long imageId, int width, int height) {
        super(imageView);
        this.hash = hash;
        this.imageId = imageId;
        this.width = width;
        this.height = height;
    }

    public static String getImageTag(ImageView imageView) {
        return (String) imageView.getTag();
    }

    public static boolean isResetRequired(ImageView imageView, String hash) {
        return !TextUtils.equals(getImageTag(imageView), hash);
    }

    @Override
    public void executeBackground() throws Throwable {
        ImageView image = getWeakObject();
        if (image != null) {
            bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true);
            if(bitmap == null) {
                int thumbnailKind = BitmapCache.getInstance().isLowDensity() ?
                        MediaStore.Images.Thumbnails.MICRO_KIND : MediaStore.Images.Thumbnails.MINI_KIND;
                Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                        image.getContext().getContentResolver(), imageId, thumbnailKind, null);
                if(thumbnail != null) {
                    BitmapCache.getInstance().saveBitmapSync(hash, thumbnail, Bitmap.CompressFormat.JPEG);
                    thumbnail.recycle();
                    bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true);
                }
            }
        }
    }

    @Override
    public void onSuccessMain() {
        ImageView image = getWeakObject();
        // Hash may be changed in another task.
        if (image != null && bitmap != null && TextUtils.equals(getImageTag(image), hash)) {
            image.setImageBitmap(bitmap);
        }
    }
}
