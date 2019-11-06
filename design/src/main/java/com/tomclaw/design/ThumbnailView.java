package com.tomclaw.design;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by Solkin on 05.11.2014.
 */
public class ThumbnailView extends AppCompatImageView implements LazyImageView {

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
