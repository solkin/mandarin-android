package com.tomclaw.mandarin.main.views.history;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.tomclaw.helpers.FileHelper;
import com.tomclaw.helpers.StringUtil;
import com.tomclaw.mandarin.main.ChatHistoryItem;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryFileView extends BaseHistoryContentView {

    private TextView name;
    private TextView size;
    private RoundCornerProgressBar progress;
    private ImageView fileType;
    private View bubbleBack;

    public BaseHistoryFileView(View itemView) {
        super(itemView);
        name = (TextView) findViewById(getNameViewId());
        size = (TextView) findViewById(getSizeViewId());
        progress = (RoundCornerProgressBar) findViewById(getProgressViewId());
        fileType = (ImageView) findViewById(getFileTypeViewId());
        bubbleBack = findViewById(getBubbleBackViewId());
    }

    protected abstract int getNameViewId();

    protected abstract int getSizeViewId();

    protected abstract int getProgressViewId();

    protected abstract int getFileTypeViewId();

    protected abstract int getBubbleBackViewId();

    protected abstract Drawable getBubbleBackground();

    @Override
    protected void afterStates(ChatHistoryItem historyItem) {
        super.afterStates(historyItem);
        name.setText(historyItem.getContentName());
        size.setText(StringUtil.formatBytes(getResources(), historyItem.getContentSize()));
        progress.setProgress(Math.max(historyItem.getContentProgress(), 5));
        bubbleBack.setBackgroundDrawable(getBubbleBackground());
    }

    protected abstract int getIconPaused();

    protected abstract int getIconRunning();

    protected int getIconStable() {
        return FileHelper.getMimeTypeResPicture(
                FileHelper.getMimeType(getHistoryItem().getContentName()));
    }

    @Override
    protected void waiting() {
        progress.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconRunning());
    }

    @Override
    protected void interrupt() {
        progress.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconPaused());
    }

    @Override
    protected void stopped() {
        progress.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconPaused());
    }

    @Override
    protected void running() {
        progress.setVisibility(View.VISIBLE);
        size.setVisibility(View.GONE);
        fileType.setImageResource(getIconRunning());
    }

    @Override
    protected void failed() {
        progress.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconStable());
    }

    @Override
    protected void stable() {
        progress.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconStable());
    }

    @Override
    protected View getClickableView() {
        return bubbleBack;
    }
}
