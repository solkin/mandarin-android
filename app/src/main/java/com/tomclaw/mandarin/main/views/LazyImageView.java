package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by Solkin on 09.11.2014.
 */
public interface LazyImageView {

    public void setPlaceholder(int resource);

    public void setBitmap(Bitmap bitmap);

    public int getWidth();

    public int getHeight();

    public String getHash();

    public void setHash(String hash);

    public Context getContext();
}
