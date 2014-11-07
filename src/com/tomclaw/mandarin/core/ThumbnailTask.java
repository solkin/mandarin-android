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
    private long imageId;
    private int width, height;

    public ThumbnailTask(ImageView imageView, long imageId) {
        this(imageView, imageId, imageView.getWidth(), imageView.getHeight());
    }

    public ThumbnailTask(ImageView imageView, long imageId, int width, int height) {
        super(imageView);
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
            bitmap = BitmapCache.getInstance().getBitmapSync(getHash(), width, height, true);
            if(bitmap == null) {
                Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(image.getContext().getContentResolver(),
                        imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
                if(thumbnail != null) {
                    BitmapCache.getInstance().saveBitmapSync(getHash(), thumbnail, Bitmap.CompressFormat.JPEG);
                    thumbnail.recycle();
                    bitmap = BitmapCache.getInstance().getBitmapSync(getHash(), width, height, true);
                }
            }
        }
    }

    @Override
    public void onSuccessMain() {
        ImageView image = getWeakObject();
        // Hash may be changed in another task.
        if (image != null && bitmap != null && TextUtils.equals(getImageTag(image), getHash())) {
            image.setImageBitmap(bitmap);
        }
    }

    public String getHash() {
        return getHash(imageId);
    }

    public static String getHash(long imageId) {
        return "thumb_" + imageId;
    }
}
