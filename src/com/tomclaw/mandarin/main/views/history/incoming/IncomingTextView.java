package com.tomclaw.mandarin.main.views.history.incoming;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.views.history.BaseHistoryTextView;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingTextView extends BaseHistoryTextView {

    public IncomingTextView(View itemView) {
        super(itemView);
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
    protected boolean hasDeliveryState() {
        return false;
    }
}
