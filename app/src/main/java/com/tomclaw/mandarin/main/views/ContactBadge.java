package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.tomclaw.design.LazyImageView;

/**
 * Created by Solkin on 10.11.2014.
 */
public class ContactBadge extends AppCompatImageView implements LazyImageView {

    private int cachedPlaceholderRes;
    private Bitmap cachedPlaceholder;

    public ContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPlaceholder(int resource) {
        if (cachedPlaceholderRes != resource) {
            cachedPlaceholderRes = resource;
            cachedPlaceholder = BitmapFactory.decodeResource(getResources(), cachedPlaceholderRes);
        }
        setImageDrawable(getRoundedDrawable(cachedPlaceholder));
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        setImageDrawable(getRoundedDrawable(bitmap));
    }

    @Override
    public String getHash() {
        return (String) getTag();
    }

    @Override
    public void setHash(String hash) {
        setTag(hash);
    }

    private Drawable getRoundedDrawable(Bitmap bitmap) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        drawable.setCornerRadius(bitmap.getWidth() / 10);
        return drawable;
    }
}
