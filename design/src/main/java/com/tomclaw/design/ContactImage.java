package com.tomclaw.design;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by Solkin on 10.11.2014.
 */
public class ContactImage extends AppCompatImageView implements LazyImageView {

    public ContactImage(Context context, AttributeSet attrs) {
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
