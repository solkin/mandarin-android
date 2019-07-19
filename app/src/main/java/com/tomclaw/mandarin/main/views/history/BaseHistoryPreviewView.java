package com.tomclaw.mandarin.main.views.history;

import android.view.View;
import android.widget.ImageView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.design.BubbleImageView;
import com.tomclaw.design.CircleProgressBar;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryPreviewView extends BaseHistoryContentView {

    private BubbleImageView previewImage;
    private CircleProgressBar progress;
    private View bubbleBack;
    private ImageView overlay;

    public BaseHistoryPreviewView(View itemView) {
        super(itemView);
        previewImage = (BubbleImageView) findViewById(getPreviewImageViewId());
        progress = (CircleProgressBar) findViewById(getProgressViewId());
        overlay = (ImageView) findViewById(getOverlayViewId());
        bubbleBack = findViewById(getBubbleBackViewId());
    }

    protected abstract int getPreviewImageViewId();

    protected abstract int getProgressViewId();

    protected abstract int getBubbleBackViewId();

    protected abstract int getOverlayViewId();

    @Override
    protected void afterStates(ChatHistoryItem historyItem) {
        super.afterStates(historyItem);
        int previewSize = getResources().getDimensionPixelSize(R.dimen.preview_size);
        BitmapCache.getInstance().getBitmapAsync(previewImage,
                historyItem.getPreviewHash(), getThumbnailPlaceholder(), previewSize, previewSize);
        if (historyItem.getContentProgress() == 0) {
            progress.setProgress(historyItem.getContentProgress());
        }
        progress.setProgressWithAnimation(historyItem.getContentProgress());
    }

    protected abstract int getThumbnailPlaceholder();

    protected abstract int getOverlayPaused();

    protected abstract int getOverlayRunning();

    protected abstract int getOverlayStable();

    @Override
    protected void waiting() {
        progress.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayRunning());
    }

    @Override
    protected void interrupt() {
        progress.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayPaused());
    }

    @Override
    protected void stopped() {
        progress.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayPaused());
    }

    @Override
    protected void running() {
        progress.setVisibility(View.VISIBLE);
        overlay.setImageResource(getOverlayRunning());
    }

    @Override
    protected void failed() {
        progress.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayStable());
    }

    @Override
    protected void stable() {
        progress.setVisibility(View.GONE);
        overlay.setImageResource(getOverlayStable());
    }

    @Override
    protected View getClickableView() {
        return bubbleBack;
    }
}
