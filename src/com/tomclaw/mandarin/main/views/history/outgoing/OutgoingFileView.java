package com.tomclaw.mandarin.main.views.history.outgoing;

import android.content.Context;
import android.util.AttributeSet;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.views.history.BaseHistoryFileView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingFileView extends BaseHistoryFileView {

    public OutgoingFileView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
    protected int getPercentViewId() {
        return R.id.out_percent;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.out_progress;
    }

    @Override
    protected int getProgressContainerViewId() {
        return R.id.out_progress_container;
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
    protected int getTimeViewId() {
        return R.id.out_time;
    }

    @Override
    protected boolean hasDeliveryState() {
        return true;
    }
}
