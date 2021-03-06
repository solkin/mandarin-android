package com.tomclaw.mandarin.main.views.history;

import android.view.View;

import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryContentView extends BaseHistoryView {

    private ChatHistoryAdapter.ContentMessageClickListener contentClickListener;

    @SuppressWarnings("WeakerAccess")
    public BaseHistoryContentView(View itemView) {
        super(itemView);
    }

    @Override
    public final void bind(ChatHistoryItem historyItem) {
        super.bind(historyItem);
        beforeStates(historyItem);
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
        // Check for selection mode right now and we shouldn't
        // override click listener for content.
        if (!getSelectionHelper().isSelectionMode()) {
            bindContentClickListener();
        }
    }

    protected void beforeStates(ChatHistoryItem historyItem) {
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
    }

    @SuppressWarnings("WeakerAccess")
    protected ChatHistoryAdapter.ContentMessageClickListener getContentClickListener() {
        return contentClickListener;
    }

    @SuppressWarnings("WeakerAccess")
    protected void bindContentClickListener() {
        getClickableView().setOnClickListener(v -> {
            ChatHistoryAdapter.ContentMessageClickListener listener = getContentClickListener();
            if (listener != null) {
                listener.onClicked(getHistoryItem());
            }
        });
    }
}
