package com.tomclaw.mandarin.main.views.history.outgoing;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.BubbleColorDrawable;
import com.tomclaw.mandarin.main.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryFileView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingFileView extends BaseHistoryFileView {

    private final Drawable background;
    private View errorView;

    public OutgoingFileView(View itemView) {
        super(itemView);
        Drawable failedBackground = new BubbleColorDrawable(itemView.getContext(),
                getResources().getColor(R.color.failed_preview_tint), Corner.RIGHT);
        errorView = findViewById(R.id.out_error);
        errorView.setBackgroundDrawable(failedBackground);
        background = new BubbleColorDrawable(itemView.getContext(),
                getStyledColor(R.attr.chat_out_bubble_color), Corner.RIGHT);
    }

    @Override
    protected int getNameViewId() {
        return R.id.out_name;
    }

    @Override
    protected int getSizeViewId() {
        return R.id.out_size;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.out_progress;
    }

    @Override
    protected int getFileTypeViewId() {
        return R.id.out_file_type;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.out_bubble_back;
    }

    @Override
    protected Drawable getBubbleBackground() {
        return background;
    }

    @Override
    protected int getIconPaused() {
        return R.drawable.files_upload;
    }

    @Override
    protected int getIconRunning() {
        return R.drawable.files_pause;
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
