package com.tomclaw.mandarin.main;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 22.09.13
 * Time: 18:55
 */
public class ChatListView extends ListView {

    private DataChangedListener dataChangedListener;

    public ChatListView(Context context) {
        super(context);
    }

    public ChatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void layoutChildren() {
        try {
            super.layoutChildren();
        } catch (Throwable ignored) {
        }
        // This will check for the messages on the
        // screen and read them if they are visible.
        if (dataChangedListener != null) {
            dataChangedListener.onDataChanged();
        }
    }

    public void setOnDataChangedListener(DataChangedListener dataChangedListener) {
        this.dataChangedListener = dataChangedListener;
    }

    public interface DataChangedListener {

        public void onDataChanged();
    }
}
