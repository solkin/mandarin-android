package com.tomclaw.mandarin.main.views.history.incoming;

import android.content.Context;
import android.util.AttributeSet;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.views.history.BaseHistoryFileView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingFileView extends BaseHistoryFileView {

    public IncomingFileView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
    protected int getPercentViewId() {
        return R.id.inc_percent;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.inc_progress;
    }

    @Override
    protected int getProgressContainerViewId() {
        return R.id.inc_progress_container;
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
