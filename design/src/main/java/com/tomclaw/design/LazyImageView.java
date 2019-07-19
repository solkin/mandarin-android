package com.tomclaw.design;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by Solkin on 09.11.2014.
 */
public interface LazyImageView {

    void setPlaceholder(int resource);

    void setBitmap(Bitmap bitmap);

    int getWidth();

    int getHeight();

    String getHash();

    void setHash(String hash);

    Context getContext();
}
