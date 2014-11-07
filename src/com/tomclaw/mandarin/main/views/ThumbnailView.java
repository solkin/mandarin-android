package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Solkin on 05.11.2014.
 */
public class ThumbnailView extends ImageView {

    public ThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        setScaleType(ScaleType.CENTER_CROP);
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageResource(int resId) {
        setScaleType(ScaleType.CENTER);
        super.setImageResource(resId);
    }
}
