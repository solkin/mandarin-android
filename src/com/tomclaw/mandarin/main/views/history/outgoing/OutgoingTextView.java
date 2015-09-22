package com.tomclaw.mandarin.main.views.history.outgoing;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.BubbleColorDrawable;
import com.tomclaw.mandarin.main.Corner;
import com.tomclaw.mandarin.main.views.history.BaseHistoryTextView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingTextView extends BaseHistoryTextView {

    private View bubbleBack;
    private BubbleColorDrawable background;

    public OutgoingTextView(View itemView) {
        super(itemView);
        bubbleBack = findViewById(R.id.out_bubble_back);
        background = new BubbleColorDrawable(itemView.getContext(), 0xffffffff, Corner.RIGHT);
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
    protected View getBubbleBack() {
        return bubbleBack;
    }

    @Override
    protected boolean hasDeliveryState() {
        return true;
    }
}
