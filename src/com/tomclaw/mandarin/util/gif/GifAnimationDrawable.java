/**
 * Copyright (C) 2013 Orthogonal Labs, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomclaw.mandarin.util.gif;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

/**
 * Creates an AnimationDrawable from a GIF image.
 *
 * @author Femi Omojola <femi@hipmob.com>
 */
public class GifAnimationDrawable extends AnimationDrawable {
    private static final String TAG = GifAnimationDrawable.class.getName();

    private boolean mDecoded;
    private GifDecoder mGifDecoder;
    private Bitmap mTmpBitmap;
    private Resources mRes;
    private int mHeight, mWidth;

    private Runnable mLoader = new Runnable() {
        public void run() {
            mGifDecoder.complete();
            int i, n = mGifDecoder.getFrameCount(), t;
            for (i = 1; i < n; i++) {
                mTmpBitmap = mGifDecoder.getFrame(i);
                t = mGifDecoder.getDelay(i);
                addFrame(new BitmapDrawable(mRes, mTmpBitmap), t);
            }
            mDecoded = true;
            mGifDecoder = null;
        }
    };

    public GifAnimationDrawable(Resources res, File f) throws IOException {
        this(res, f, false);
    }

    public GifAnimationDrawable(Resources res, InputStream is) throws IOException {
        this(res, is, false);
    }

    public GifAnimationDrawable(Resources res, File f, boolean inline) throws IOException {
        this(res, new BufferedInputStream(new FileInputStream(f), 32768), inline);
    }

    public GifAnimationDrawable(Resources res, InputStream is, boolean inline) throws IOException {
        super();

        InputStream bis;
        if (!BufferedInputStream.class.isInstance(is)) {
            bis = new BufferedInputStream(is, 32768);
        } else {
            bis = is;
        }

        mRes = res;
        mDecoded = false;
        mGifDecoder = new GifDecoder();
        mGifDecoder.read(bis);

        mTmpBitmap = mGifDecoder.getFrame(0);
        mHeight = mTmpBitmap.getHeight();
        mWidth = mTmpBitmap.getWidth();
        addFrame(new BitmapDrawable(mRes, mTmpBitmap), mGifDecoder.getDelay(0));

        setOneShot(mGifDecoder.getLoopCount() != 0);

        if (inline) {
            mLoader.run();
        } else {
            new Thread(mLoader).start();
        }
    }

    public void recycle() {
        stop();
        for (int i = 0; i < getNumberOfFrames(); i++) {
            BitmapDrawable drawable = (BitmapDrawable) getFrame(i);
            Bitmap bm = drawable.getBitmap();
            bm.recycle();
        }
    }

    public boolean isDecoded() {
        return mDecoded;
    }

    public int getMinimumHeight() {
        return mHeight;
    }

    public int getMinimumWidth() {
        return mWidth;
    }

    public int getIntrinsicHeight() {
        return mHeight;
    }

    public int getIntrinsicWidth() {
        return mWidth;
    }
}
