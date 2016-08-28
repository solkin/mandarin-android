package com.tomclaw.mandarin.main.views.history.outgoing;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.BubbleColorDrawable;
import com.tomclaw.mandarin.main.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryPreviewView;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class OutgoingPreviewView extends BaseHistoryPreviewView {

    private View errorView;

    public OutgoingPreviewView(View itemView) {
        super(itemView);
        Drawable failedBackground = new BubbleColorDrawable(itemView.getContext(),
                getResources().getColor(R.color.failed_preview_tint), Corner.RIGHT);
        errorView = findViewById(R.id.out_error);
        errorView.setBackgroundDrawable(failedBackground);
    }

    @Override
    protected int getPreviewImageViewId() {
        return R.id.out_preview_image;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.out_progress;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.out_bubble_back;
    }

    @Override
    protected int getTimeViewId() {
        return R.id.out_time;
    }

    @Override
    protected boolean hasDeliveryState() {
        return true;
    }

    @Override
    protected void waiting() {
        super.waiting();
        errorView.setVisibility(View.GONE);
    }

    @Override
    protected void interrupt() {
        super.interrupt();
        errorView.setVisibility(View.GONE);
    }

    @Override
    protected void stopped() {
        super.stopped();
        errorView.setVisibility(View.GONE);
    }

    @Override
    protected void running() {
        super.running();
        errorView.setVisibility(View.GONE);
    }

    @Override
    protected void failed() {
        super.failed();
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void stable() {
        super.stable();
        errorView.setVisibility(View.GONE);
    }
}
