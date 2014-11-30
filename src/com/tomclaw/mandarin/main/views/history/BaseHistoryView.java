package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryView extends LinearLayout {

    private static final int[] MESSAGE_STATES = new int[]{
            R.drawable.ic_dot,
            R.drawable.ic_error,
            R.drawable.ic_dot,
            R.drawable.ic_sent,
            R.drawable.ic_delivered
    };

    private View dateLayout;
    private TextView messageDate;
    private ImageView deliveryState;
    private TextView timeView;

    private ChatHistoryItem historyItem;

    public BaseHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        dateLayout = findViewById(getDateLayoutViewId());
        messageDate = (TextView) findViewById(getDateTextViewId());
        if (hasDeliveryState()) {
            deliveryState = (ImageView) findViewById(R.id.message_delivery);
        }
        timeView = (TextView) findViewById(getTimeViewId());
    }

    protected int getDateLayoutViewId() {
        return R.id.date_layout;
    }

    protected int getDateTextViewId() {
        return R.id.message_date;
    }

    protected abstract int getTimeViewId();

    protected abstract boolean hasDeliveryState();

    public void bind(ChatHistoryItem historyItem) {
        this.historyItem = historyItem;
        if (historyItem.isDateVisible()) {
            // Update visibility.
            dateLayout.setVisibility(View.VISIBLE);
            // Update date text view.
            messageDate.setText(historyItem.getMessageDateText());
        } else {
            // Update visibility.
            dateLayout.setVisibility(GONE);
        }
        if (hasDeliveryState()) {
            deliveryState.setImageResource(MESSAGE_STATES[historyItem.getMessageState()]);
        }
        timeView.setText(historyItem.getMessageTimeText());
    }

    public ChatHistoryItem getHistoryItem() {
        return historyItem;
    }

    public void setContentClickListener(ChatHistoryAdapter.ContentMessageClickListener contentClickListener) {
    }
}
