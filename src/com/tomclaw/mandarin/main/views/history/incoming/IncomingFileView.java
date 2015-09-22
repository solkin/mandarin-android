package com.tomclaw.mandarin.main.views.history.incoming;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.BubbleColorDrawable;
import com.tomclaw.mandarin.main.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryFileView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingFileView extends BaseHistoryFileView {

    private final Drawable background;

    public IncomingFileView(View itemView) {
        super(itemView);
        background = new BubbleColorDrawable(itemView.getContext(), 0xffbbe061, Corner.LEFT);
    }

    @Override
    protected int getNameViewId() {
        return R.id.inc_name;
    }

    @Override
    protected int getSizeViewId() {
        return R.id.inc_size;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.inc_progress;
    }

    @Override
    protected int getFileTypeViewId() {
        return R.id.inc_file_type;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.inc_bubble_back;
    }

    @Override
    protected Drawable getBubbleBackground() {
        return background;
    }

    @Override
    protected int getIconPaused() {
        return R.drawable.files_download;
    }

    @Override
    protected int getIconRunning() {
        return R.drawable.files_pause;
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
