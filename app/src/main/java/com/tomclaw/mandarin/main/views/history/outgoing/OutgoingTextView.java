package com.tomclaw.mandarin.main.views.history.outgoing;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.tomclaw.mandarin.R;
import com.tomclaw.design.BubbleColorDrawable;
import com.tomclaw.design.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryTextView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingTextView extends BaseHistoryTextView {

    private Drawable background;

    public OutgoingTextView(View itemView) {
        super(itemView);
        background = new BubbleColorDrawable(itemView.getContext(), getStyledColor(R.attr.chat_out_bubble_color), Corner.RIGHT);
    }

    @Override
    protected int getTimeViewId() {
        return R.id.out_time;
    }

    @Override
    protected int getTextViewId() {
        return R.id.out_text;
    }

    @Override
    protected Drawable getBubbleBackground() {
        return background;
    }

    @Override
    protected int getBubbleBackViewId() {
        return R.id.out_bubble_back;
    }

    @Override
    protected boolean hasDeliveryState() {
        return true;
    }
}
