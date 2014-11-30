package com.tomclaw.mandarin.main.views.history.outgoing;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 30.11.2014.
 */
public class OutgoingVideoView extends OutgoingPreviewView {

    private View errorView;

    public OutgoingVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getThumbnailPlaceholder() {
        return R.drawable.video_placeholder;
    }
}
