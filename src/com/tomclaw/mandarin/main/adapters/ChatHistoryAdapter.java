package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DataProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/7/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatHistoryAdapter extends SimpleCursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ADAPTER_ID = (int) System.currentTimeMillis() / 100000;

    private static final String childFrom[] = {DataProvider.HISTORY_BUDDY_NICK, DataProvider.HISTORY_MESSAGE_TEXT};
    private static final int childTo[] = {R.id.chatBuddyNick, R.id.chatMessage};

    private Context context;
    private LoaderManager loaderManager;
    private String buddyDbId;

    public ChatHistoryAdapter(Context context, LoaderManager loaderManager, String buddyDbId) {
        super(context, R.layout.chat_item, null, childFrom, childTo, 0x00);
        this.context = context;
        this.loaderManager = loaderManager;
        this.buddyDbId = buddyDbId;
        // Initialize loader for online Id.
        this.loaderManager.initLoader(ADAPTER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context, Settings.HISTORY_RESOLVER_URI, null,
                DataProvider.HISTORY_BUDDY_DB_ID + "='" + buddyDbId + "'", null,
                DataProvider.HISTORY_MESSAGE_TIME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        swapCursor(null);
    }

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            if (!mDataValid) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                view = newView(mContext, mCursor, parent);
            } else {
                view = convertView;
            }
            bindView(view, mContext, mCursor);
        } catch (Throwable ex) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.chat_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        return view;
    }
}