package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Prevent NPE if fake dragging and touching ViewPager.
        return !isFakeDragging() && super.onInterceptTouchEvent(ev);
    }
}
