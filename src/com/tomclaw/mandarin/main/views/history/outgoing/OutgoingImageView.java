package com.tomclaw.mandarin.main.views.history.outgoing;

import android.text.TextUtils;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.util.FileHelper;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingImageView extends OutgoingPreviewView {

    private boolean isAnimated;

    public OutgoingImageView(View itemView) {
        super(itemView);
    }

    @Override
    protected void beforeStates(ChatHistoryItem historyItem) {
        isAnimated = TextUtils.equals(FileHelper.getFileExtensionFromPath(
                historyItem.getContentName()).toLowerCase(), "gif");
    }

    @Override
    protected int getOverlayViewId() {
        return R.id.out_image_overlay;
    }

    @Override
    protected int getThumbnailPlaceholder() {
        return R.drawable.picture_placeholder;
    }

    @Override
    protected int getOverlayPaused() {
        return R.drawable.chat_upload;
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
