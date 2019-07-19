package com.tomclaw.mandarin.main.views.history.incoming;

import android.text.TextUtils;
import android.view.View;

import com.tomclaw.helpers.Files;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatHistoryItem;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingImageView extends IncomingPreviewView {

    private boolean isAnimated;

    public IncomingImageView(View itemView) {
        super(itemView);
    }

    @Override
    protected void beforeStates(ChatHistoryItem historyItem) {
        isAnimated = TextUtils.equals(Files.getFileExtensionFromPath(
                historyItem.getContentName()).toLowerCase(), "gif");
    }

    @Override
    protected int getOverlayViewId() {
        return R.id.inc_image_overlay;
    }

    @Override
    protected int getThumbnailPlaceholder() {
        return R.drawable.picture_placeholder;
    }

    @Override
    protected int getOverlayPaused() {
        return R.drawable.chat_download;
    }

    @Override
    protected int getOverlayRunning() {
        return R.drawable.chat_download_cancel;
    }

    @Override
    protected int getOverlayStable() {
        return isAnimated ? R.drawable.video_play : 0;
    }
}
