package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class RosterDialogsAdapter extends SimpleCursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ADAPTER_DIALOGS_ID = 0x00;

    private static final String from[] = {RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS};
    private static final int to[] = {R.id.buddyId, R.id.buddyNick, R.id.buddyStatus};

    private Context context;
    private LoaderManager loaderManager;


    public RosterDialogsAdapter(Context context, LoaderManager loaderManager) {
        super(context, R.layout.buddy_item, null, from, to, 0x00);
        this.context = context;
        this.loaderManager = loaderManager;
        // Initialize loader for dialogs Id.
        this.loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context,
                Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
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
        try {
            if (!mDataValid) {
                Log.d(Settings.LOG_TAG, "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                Log.d(Settings.LOG_TAG, "couldn't move cursor to position " + position);
            }
        } catch (Throwable ex) {
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        View v;
        if (convertView == null) {
            v = newView(mContext, mCursor, parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, mCursor);
        return v;
    }
}
