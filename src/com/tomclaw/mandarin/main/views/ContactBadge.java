package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

/**
 * Created by Solkin on 10.11.2014.
 */
public class ContactBadge extends QuickContactBadge implements LazyImageView {

    public ContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPlaceholder(int resource) {
        setImageResource(resource);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
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
