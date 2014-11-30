package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 05.11.2014.
 */
public class ThumbnailView extends ImageView implements LazyImageView {

    public ThumbnailView(Context context, AttributeSet attrs) {
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
