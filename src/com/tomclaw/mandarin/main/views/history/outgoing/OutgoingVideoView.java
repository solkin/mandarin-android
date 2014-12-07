package com.tomclaw.mandarin.main.views.history.outgoing;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingVideoView extends OutgoingPreviewView {

    public OutgoingVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getOverlayViewId() {
        return R.id.out_video_overlay;
    }

    @Override
    protected int getThumbnailPlaceholder() {
        return R.drawable.video_placeholder;
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
        return R.drawable.video_play;
    }
}
