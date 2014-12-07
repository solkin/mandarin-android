package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
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
    private ImageView overlay;

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
        overlay = (ImageView) findViewById(getOverlayViewId());
    }

    protected abstract int getPreviewImageViewId();

    protected abstract int getProgressContainerViewId();

    protected abstract int getProgressViewId();

    protected abstract int getPercentViewId();

    protected abstract int getBubbleBackViewId();

    protected abstract int getOverlayViewId();

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

    protected abstract int getOverlayPaused();

    protected abstract int getOverlayRunning();

    protected abstract int getOverlayStable();

    @Override
    protected void waiting() {
        progressContainer.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayRunning());
    }

    @Override
    protected void interrupt() {
        progressContainer.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayPaused());
    }

    @Override
    protected void stopped() {
        progressContainer.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayPaused());
    }

    @Override
    protected void running() {
        progressContainer.setVisibility(View.VISIBLE);
        overlay.setImageResource(getOverlayRunning());
    }

    @Override
    protected void failed() {
        progressContainer.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayStable());
    }

    @Override
    protected void stable() {
        progressContainer.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayStable());
    }

    @Override
    protected View getClickableView() {
        return bubbleBack;
    }
}
