package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 08.12.13
 * Time: 22:13
 */
public class BitmapTask extends WeakObjectTask<ImageView> {

    private Bitmap bitmap;
    private String hash;
    private int width, height;

    public BitmapTask(ImageView imageView, String hash) {
        super(imageView);
        this.hash = hash;
        this.width = imageView.getWidth();
        this.height = imageView.getHeight();
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
