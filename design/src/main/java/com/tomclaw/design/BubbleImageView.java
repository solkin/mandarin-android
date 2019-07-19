package com.tomclaw.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by Solkin on 18.10.2014.
 */
public class BubbleImageView extends AppCompatImageView implements LazyImageView {

    private int placeholderTintColor;
    private int bubbleColor;
    private int bubbleCorner;
    private Corner corner;

    public BubbleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BubbleImageView,
                0, 0);
        try {
            placeholderTintColor = a.getColor(R.styleable.BubbleImageView_placeholder_tint, 0);
            bubbleColor = a.getColor(R.styleable.BubbleImageView_bubble_color, 0);
            bubbleCorner = a.getInt(R.styleable.BubbleImageView_bubble_corner, 0);
            switch (bubbleCorner) {
                case 0:
                    corner = Corner.LEFT;
                    break;
                case 1:
                    corner = Corner.RIGHT;
                    break;
                case 2:
                default:
                    corner = Corner.NONE;
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setPlaceholder(int resource) {
        setScaleType(ScaleType.CENTER);
        int previewSize = getResources().getDimensionPixelSize(R.dimen.preview_size);
        getLayoutParams().width = previewSize;
        getLayoutParams().height = previewSize;
        setBackgroundDrawable(new BubbleColorDrawable(getContext(), bubbleColor, corner));
        setColorFilter(placeholderTintColor);
        setImageResource(resource);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        setColorFilter(android.R.color.transparent);
        setBackgroundDrawable(null);
        BubbleBitmapDrawable drawable = new BubbleBitmapDrawable(
                bitmap, corner, getContext());
        setImageDrawable(drawable);
        getLayoutParams().width = drawable.getIntrinsicWidth();
        getLayoutParams().height = drawable.getIntrinsicHeight();
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
