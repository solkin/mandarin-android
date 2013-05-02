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
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.ProviderAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class RosterOnlineAdapter extends SimpleCursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor>, ProviderAdapter {

    private static final int ADAPTER_ONLINE_ID = -3;

    private static final String childFrom[] = {RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK,
            RosterProvider.ROSTER_BUDDY_STATUS};
    private static final int childTo[] = {R.id.buddyId, R.id.buddyNick, R.id.buddyStatus};

    private Context context;
    private LoaderManager loaderManager;

    public RosterOnlineAdapter(Context context, LoaderManager loaderManager) {
        super(context, R.layout.buddy_item, null, childFrom, childTo, 0x00);
        this.context = context;
        this.loaderManager = loaderManager;
        // Initialize loader for online Id.
        this.loaderManager.initLoader(ADAPTER_ONLINE_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context, Settings.BUDDY_RESOLVER_URI, null,
                RosterProvider.ROSTER_BUDDY_STATE + "='" + 1 + "'", null,
                RosterProvider.ROSTER_BUDDY_NICK + " ASC");
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
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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

    @Override
    public void destroyLoader() {
        loaderManager.destroyLoader(ADAPTER_ONLINE_ID);
    }
}
