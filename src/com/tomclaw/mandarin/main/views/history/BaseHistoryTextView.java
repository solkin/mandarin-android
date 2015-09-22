package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.tomclaw.mandarin.main.ChatHistoryItem;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryTextView extends BaseHistoryView {

    private TextView textView;

    public BaseHistoryTextView(View itemView) {
        super(itemView);
        textView = (TextView) findViewById(getTextViewId());
    }

    protected abstract int getTextViewId();

    @Override
    public void bind(ChatHistoryItem historyItem) {
        super.bind(historyItem);
        textView.setText(historyItem.getMessageText());
    }
}
