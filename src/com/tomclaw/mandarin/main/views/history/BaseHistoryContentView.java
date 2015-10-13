package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryContentView extends BaseHistoryView {

    private ChatHistoryAdapter.ContentMessageClickListener contentClickListener;

    public BaseHistoryContentView(View itemView) {
        super(itemView);
    }

    @Override
    public final void bind(ChatHistoryItem historyItem) {
        super.bind(historyItem);
        switch (historyItem.getContentState()) {
            case GlobalProvider.HISTORY_CONTENT_STATE_WAITING: {
                waiting();
                break;
            }
            case GlobalProvider.HISTORY_CONTENT_STATE_INTERRUPT: {
                interrupt();
                break;
            }
            case GlobalProvider.HISTORY_CONTENT_STATE_STOPPED: {
                stopped();
                break;
            }
            case GlobalProvider.HISTORY_CONTENT_STATE_RUNNING: {
                running();
                break;
            }
            case GlobalProvider.HISTORY_CONTENT_STATE_FAILED: {
                failed();
                break;
            }
            case GlobalProvider.HISTORY_CONTENT_STATE_STABLE: {
                stable();
                break;
            }
        }
        afterStates(historyItem);
        bindContentClickListener();
    }

    protected void afterStates(ChatHistoryItem historyItem) {
    }

    protected abstract void waiting();

    protected abstract void interrupt();

    protected abstract void stopped();

    protected abstract void running();

    protected abstract void failed();

    protected abstract void stable();

    @Override
    public void setContentClickListener(ChatHistoryAdapter.ContentMessageClickListener contentClickListener) {
        this.contentClickListener = contentClickListener;
        bindContentClickListener();
    }

    protected ChatHistoryAdapter.ContentMessageClickListener getContentClickListener() {
        return contentClickListener;
    }

    protected void bindContentClickListener() {
        getClickableView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatHistoryAdapter.ContentMessageClickListener listener = getContentClickListener();
                if (listener != null) {
                    listener.onClicked(getHistoryItem());
                }
            }
        });
    }
}
