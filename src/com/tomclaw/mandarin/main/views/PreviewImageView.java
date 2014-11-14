package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Solkin on 18.10.2014.
 */
public class PreviewImageView extends ImageView implements LazyImageView {

    public PreviewImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPlaceholder(int resource) {
        setScaleType(ScaleType.CENTER);
        setImageResource(resource);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        setScaleType(ScaleType.CENTER_CROP);
        setImageBitmap(bitmap);
    }

    @Override
    public String getHash() {
        return (String) getTag();
    }

    @Override
    public void setHash(String hash) {
        setTag(hash);
    }
}
