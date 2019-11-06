package com.tomclaw.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by Solkin on 18.10.2014.
 */
public class PreviewImageView extends AppCompatImageView implements LazyImageView {

    private int placeholderTintColor;

    public PreviewImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PreviewImageView,
                0, 0);
        try {
            placeholderTintColor = a.getColor(R.styleable.PreviewImageView_preview_placeholder_tint, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setPlaceholder(int resource) {
        setScaleType(ScaleType.CENTER);
        setColorFilter(placeholderTintColor);
        setImageResource(resource);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        setScaleType(ScaleType.CENTER_CROP);
        setColorFilter(android.R.color.transparent);
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
