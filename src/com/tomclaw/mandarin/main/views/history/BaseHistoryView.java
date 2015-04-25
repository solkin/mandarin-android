package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryView extends LinearLayout {

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
            float alpha;
            int stateImage;
            switch (historyItem.getMessageState()) {
                case GlobalProvider.HISTORY_MESSAGE_STATE_ERROR:
                    alpha = 1f;
                    stateImage = R.drawable.ic_error;
                    break;
                case GlobalProvider.HISTORY_MESSAGE_STATE_UNDETERMINED:
                case GlobalProvider.HISTORY_MESSAGE_STATE_SENDING:
                    alpha = 0.45f;
                    stateImage = 0;
                    break;
                case GlobalProvider.HISTORY_MESSAGE_STATE_SENT:
                    alpha = 1f;
                    stateImage = 0;
                    break;
                case GlobalProvider.HISTORY_MESSAGE_STATE_DELIVERED:
                    alpha = 1f;
                    stateImage = R.drawable.ic_delivered;
                    break;
                default:
                    alpha = 1f;
                    stateImage = 0;
            }
            if (stateImage == 0) {
                deliveryState.setVisibility(INVISIBLE);
            } else {
                deliveryState.setVisibility(VISIBLE);
                deliveryState.setImageResource(stateImage);
            }
            setAlpha(alpha);
        }
        timeView.setText(historyItem.getMessageTimeText());
    }

    public ChatHistoryItem getHistoryItem() {
        return historyItem;
    }

    public void setContentClickListener(ChatHistoryAdapter.ContentMessageClickListener contentClickListener) {
    }
}
