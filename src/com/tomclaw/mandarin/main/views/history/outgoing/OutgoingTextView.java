package com.tomclaw.mandarin.main.views.history.outgoing;

import android.content.Context;
import android.util.AttributeSet;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.views.history.BaseHistoryTextView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingTextView extends BaseHistoryTextView {

    public OutgoingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
    protected boolean hasDeliveryState() {
        return true;
    }
}
