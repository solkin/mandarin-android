package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Solkin on 26.12.2014.
 */
public class FixedViewPager extends ViewPager {

    public FixedViewPager(Context context) {
        super(context);
    }

    public FixedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
            Logger.log("Some strange error in ViewPager (onTouchEvent)", ignored);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            // Prevent NPE if fake dragging and touching ViewPager.
            return !isFakeDragging() && super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
            Logger.log("Some strange error in ViewPager (onInterceptTouchEvent)", ignored);
        }
        return false;
    }
}
