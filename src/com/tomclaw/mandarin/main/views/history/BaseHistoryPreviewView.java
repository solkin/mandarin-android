package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.views.PreviewImageView;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryPreviewView extends BaseHistoryContentView {

    private PreviewImageView previewImage;
    private View progressContainer;
    private ProgressBar progress;
    private TextView percent;
    private View bubbleBack;

    public BaseHistoryPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        previewImage = (PreviewImageView) findViewById(getPreviewImageViewId());
        progressContainer = findViewById(getProgressContainerViewId());
        progress = (ProgressBar) findViewById(getProgressViewId());
        percent = (TextView) findViewById(getPercentViewId());
    }

    protected abstract int getPreviewImageViewId();

    protected abstract int getProgressContainerViewId();

    protected abstract int getProgressViewId();

    protected abstract int getPercentViewId();

    protected abstract int getBubbleBackViewId();

    @Override
    protected void afterStates(ChatHistoryItem historyItem) {
        super.afterStates(historyItem);
        BitmapCache.getInstance().getBitmapAsync(previewImage,
                historyItem.getPreviewHash(), getThumbnailPlaceholder(), true);
        progress.setProgress(historyItem.getContentProgress());
        percent.setText(historyItem.getContentProgress() + "%");
        bubbleBack = findViewById(getBubbleBackViewId());
    }

    protected abstract int getThumbnailPlaceholder();

    @Override
    protected void waiting() {
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    protected void interrupt() {
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    protected void stopped() {
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    protected void running() {
        progressContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void failed() {
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    protected void stable() {
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    protected View getClickableView() {
        return bubbleBack;
    }
}
