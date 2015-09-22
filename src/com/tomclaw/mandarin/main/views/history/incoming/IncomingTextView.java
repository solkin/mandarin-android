package com.tomclaw.mandarin.main.views.history.incoming;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.BubbleColorDrawable;
import com.tomclaw.mandarin.main.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryTextView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingTextView extends BaseHistoryTextView {

    private Drawable background;

    public IncomingTextView(View itemView) {
        super(itemView);
        background = new BubbleColorDrawable(itemView.getContext(), 0xffbbe061, Corner.LEFT);
    }

    @Override
    protected int getTimeViewId() {
        return R.id.inc_time;
    }

    @Override
    protected int getTextViewId() {
        return R.id.inc_text;
    }

    @Override
    protected Drawable getBubbleBackground() {
        return background;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.inc_bubble_back;
    }

    @Override
    protected boolean hasDeliveryState() {
        return false;
    }
}
