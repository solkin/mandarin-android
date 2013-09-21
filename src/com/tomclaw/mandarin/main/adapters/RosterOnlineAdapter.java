package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:22 PM
 */
public class RosterOnlineAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_ONLINE_ID = -3;

    /**
     * Columns
     */
    private static int COLUMN_ROSTER_BUDDY_ID;
    private static int COLUMN_ROSTER_BUDDY_NICK;
    private static int COLUMN_ROSTER_BUDDY_STATUS;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;

    public RosterOnlineAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        // Initialize loader for online Id.
        loaderManager.initLoader(ADAPTER_ONLINE_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context, Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_STATUS + "!='" + StatusUtil.STATUS_OFFLINE + "'", null,
                GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROSTER_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
        COLUMN_ROSTER_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
        COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
        COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE);
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
            if (!getCursor().moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                v = newView(context, getCursor(), parent);
            } else {
                v = convertView;
            }
            bindView(v, context, getCursor());
        } catch (Throwable ex) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = mInflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
            ex.printStackTrace();
        }
        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.buddy_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Setup values
        ((TextView) view.findViewById(R.id.buddy_id)).setText(cursor.getString(COLUMN_ROSTER_BUDDY_ID));
        ((TextView) view.findViewById(R.id.buddy_nick)).setText(cursor.getString(COLUMN_ROSTER_BUDDY_NICK));
        ((ImageView) view.findViewById(R.id.buddy_status)).setImageResource(
                StatusUtil.getStatusResource(
                        cursor.getString(COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE),
                        cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS)));
    }

    public int getBuddyDbId(int position) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return getCursor().getInt(getCursor().getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }
}
