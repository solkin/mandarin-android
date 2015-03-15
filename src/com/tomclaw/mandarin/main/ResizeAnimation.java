package com.tomclaw.mandarin.main;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
* Created by Solkin on 15.03.2015.
*/
public class ResizeAnimation extends Animation {
    private final int startHeight;
    private final int targetHeight;
    private View view;

    public ResizeAnimation(View view, int targetHeight) {
        this.view = view;
        this.targetHeight = targetHeight;
        startHeight = view.getHeight();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.getLayoutParams().height = (int) (startHeight + (targetHeight - startHeight) * interpolatedTime);
        view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
