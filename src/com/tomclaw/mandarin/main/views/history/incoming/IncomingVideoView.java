package com.tomclaw.mandarin.main.views.history.incoming;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingVideoView extends IncomingPreviewView {

    public IncomingVideoView(View itemView) {
        super(itemView);
    }

    @Override
    protected int getOverlayViewId() {
        return R.id.inc_video_overlay;
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
