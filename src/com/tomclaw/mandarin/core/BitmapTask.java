package com.tomclaw.mandarin.core;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 08.12.13
 * Time: 22:13
 */
public class BitmapTask extends Task {

    private final WeakReference<ImageView> imageWeakReference;
    private Bitmap bitmap;
    private String hash;
    private int width, height;

    public BitmapTask(ImageView imageView, String hash) {
        this.imageWeakReference = new WeakReference<ImageView>(imageView);
        this.hash = hash;
        this.width = imageView.getWidth();
        this.height = imageView.getHeight();
    }

    public static boolean isResetRequired(ImageView imageView, String hash) {
        String tagHashValue = (String) imageView.getTag();
        return !TextUtils.isEmpty(tagHashValue) && TextUtils.isEmpty(hash);
    }

    @Override
    public void executeBackground() throws Throwable {
        ImageView image = imageWeakReference.get();
        if(image != null) {
            bitmap = BitmapCache.getInstance().getBitmapSync(hash, width, height, true);
        }
    }

    @Override
    public void onSuccessMain() {
        ImageView image = imageWeakReference.get();
        if(image != null && bitmap != null) {
            /*Drawable[] layers = new Drawable[] {
                image.getDrawable(), new BitmapDrawable(Resources.getSystem(), bitmap)
            };
            TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
            image.setImageDrawable(transitionDrawable);
            image.setTag(hash);
            transitionDrawable.startTransition(700);*/
            image.setTag(hash);
            image.setImageBitmap(bitmap);
        }
    }
}
