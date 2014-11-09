package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;
import com.tomclaw.mandarin.main.views.LazyImageView;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 08.12.13
 * Time: 22:13
 */
public class BitmapTask extends WeakObjectTask<LazyImageView> {

    private Bitmap bitmap;
    private String hash;
    private int width, height;

    public BitmapTask(LazyImageView imageView, String hash) {
        this(imageView, hash, imageView.getWidth(), imageView.getHeight());
    }

    public BitmapTask(LazyImageView imageView, String hash, int width, int height) {
        super(imageView);
        this.hash = hash;
        this.width = width;
        this.height = height;
    }

    public static boolean isResetRequired(LazyImageView imageView, String hash) {
        return !TextUtils.equals(imageView.getHash(), hash);
    }

    @Override
    public void executeBackground() throws Throwable {
        LazyImageView image = getWeakObject();
        if (image != null) {
            bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true);
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
