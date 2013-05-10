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
import com.tomclaw.mandarin.core.GlobalProvider;
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

    private static final int ADAPTER_DIALOGS_ID = -2;

    private static final String from[] = {GlobalProvider.ROSTER_BUDDY_ID, GlobalProvider.ROSTER_BUDDY_NICK, GlobalProvider.ROSTER_BUDDY_STATUS};
    private static final int to[] = {R.id.buddy_id, R.id.buddy_nick, R.id.buddy_status};

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
                Settings.BUDDY_RESOLVER_URI, null, GlobalProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, GlobalProvider.ROSTER_BUDDY_STATE + " DESC," + GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v;
        try {
            if (!mDataValid) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                v = newView(mContext, mCursor, parent);
            } else {
                v = convertView;
            }
            bindView(v, mContext, mCursor);
        } catch (Throwable ex) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = mInflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        return v;
    }

    public long getBuddyDbId(int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return mCursor.getLong(mCursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }
}
