package com.tomclaw.mandarin.main.views.history;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryFileView extends BaseHistoryContentView {

    private TextView name;
    private TextView size;
    private TextView percent;
    private ProgressBar progress;
    private View progressContainer;
    private ImageView fileType;
    private View bubbleBack;

    public BaseHistoryFileView(View itemView) {
        super(itemView);
        name = (TextView) findViewById(getNameViewId());
        size = (TextView) findViewById(getSizeViewId());
        percent = (TextView) findViewById(getPercentViewId());
        progress = (ProgressBar) findViewById(getProgressViewId());
        progressContainer = findViewById(getProgressContainerViewId());
        fileType = (ImageView) findViewById(getFileTypeViewId());
        bubbleBack = findViewById(getBubbleBackViewId());
    }

    protected abstract int getNameViewId();

    protected abstract int getSizeViewId();

    protected abstract int getPercentViewId();

    protected abstract int getProgressViewId();

    protected abstract int getProgressContainerViewId();

    protected abstract int getFileTypeViewId();

    protected abstract int getBubbleBackViewId();

    @Override
    protected void afterStates(ChatHistoryItem historyItem) {
        super.afterStates(historyItem);
        name.setText(historyItem.getContentName());
        size.setText(StringUtil.formatBytes(getResources(), historyItem.getContentSize()));
        progress.setProgress(historyItem.getContentProgress());
        percent.setText(historyItem.getContentProgress() + "%");
    }

    protected abstract int getIconPaused();

    protected abstract int getIconRunning();

    protected int getIconStable() {
        return FileHelper.getMimeTypeResPicture(
                FileHelper.getMimeType(getHistoryItem().getContentName()));
    }

    @Override
    protected void waiting() {
        progressContainer.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconRunning());
    }

    @Override
    protected void interrupt() {
        progressContainer.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconPaused());
    }

    @Override
    protected void stopped() {
        progressContainer.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconPaused());
    }

    @Override
    protected void running() {
        progressContainer.setVisibility(View.VISIBLE);
        size.setVisibility(View.GONE);
        fileType.setImageResource(getIconRunning());
    }

    @Override
    protected void failed() {
        progressContainer.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconStable());
    }

    @Override
    protected void stable() {
        progressContainer.setVisibility(View.GONE);
        size.setVisibility(View.VISIBLE);
        fileType.setImageResource(getIconStable());
    }

    @Override
    protected View getClickableView() {
        return bubbleBack;
    }
}
