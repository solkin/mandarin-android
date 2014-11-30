package com.tomclaw.mandarin.main.views.history.incoming;

import android.content.Context;
import android.util.AttributeSet;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.views.history.BaseHistoryPreviewView;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class IncomingPreviewView extends BaseHistoryPreviewView {

    public IncomingPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getPreviewImageViewId() {
        return R.id.inc_preview_image;
    }

    @Override
    protected int getProgressContainerViewId() {
        return R.id.inc_progress_container;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.inc_progress;
    }

    @Override
    protected int getPercentViewId() {
        return R.id.inc_percent;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.inc_bubble_back;
    }

    @Override
    protected int getTimeViewId() {
        return R.id.inc_time;
    }

    @Override
    protected boolean hasDeliveryState() {
        return false;
    }
}
