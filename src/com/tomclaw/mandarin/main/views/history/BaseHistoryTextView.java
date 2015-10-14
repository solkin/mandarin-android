package com.tomclaw.mandarin.main.views.history;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.tomclaw.mandarin.main.ChatHistoryItem;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryTextView extends BaseHistoryView {

    private View bubbleBack;
    private TextView textView;

    public BaseHistoryTextView(View itemView) {
        super(itemView);
        bubbleBack = findViewById(getBubbleBackViewId());
        textView = (TextView) findViewById(getTextViewId());
    }

    protected abstract int getTextViewId();

    protected abstract Drawable getBubbleBackground();

    protected abstract int getBubbleBackViewId();

    @Override
    public void bind(ChatHistoryItem historyItem) {
        textView.setTextIsSelectable(!getSelectionHelper().isSelectionMode());
        super.bind(historyItem);
        textView.setText(historyItem.getMessageText());
        bubbleBack.setBackgroundDrawable(getBubbleBackground());
    }

    @Override
    protected View getClickableView() {
        return textView;
    }
}
