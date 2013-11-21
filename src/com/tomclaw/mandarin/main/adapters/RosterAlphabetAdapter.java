package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
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
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.im.StatusUtil;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 17:44
 */
public class RosterAlphabetAdapter extends CursorAdapter
        implements LoaderManager.LoaderCallbacks<Cursor>, StickyListHeadersAdapter {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_ALPHABET_ID = -1;

    /**
     * Filter
     */
    public static final int FILTER_ALL_BUDDIES = 0x00;
    public static final int FILTER_ONLINE_ONLY = 0x01;

    /**
     * Columns
     */
    private static int COLUMN_ROSTER_BUDDY_ID;
    private static int COLUMN_ROSTER_BUDDY_NICK;
    private static int COLUMN_ROSTER_BUDDY_STATUS;
    private static int COLUMN_ROSTER_BUDDY_STATUS_TITLE;
    private static int COLUMN_ROSTER_BUDDY_STATUS_MESSAGE;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE;
    private static int COLUMN_ROSTER_BUDDY_ALPHABET_INDEX;
    private static int COLUMN_ROSTER_BUDDY_UNREAD_COUNT;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;
    private int filter;
    private boolean isShowTemp = false;
    private LoaderManager loaderManager;

    public RosterAlphabetAdapter(Activity context, LoaderManager loaderManager, int filter) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        this.loaderManager = loaderManager;
        this.filter = filter;
        this.isShowTemp = PreferenceHelper.isShowTemp(context);
        initLoader();
    }

    public void initLoader() {
        // Initialize loader for dialogs Id.
        loaderManager.restartLoader(ADAPTER_ALPHABET_ID, null, this);
    }

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
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
        }
        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.buddy_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Setup values
        ((TextView) view.findViewById(R.id.buddy_nick)).setText(cursor.getString(COLUMN_ROSTER_BUDDY_NICK));
        ((ImageView) view.findViewById(R.id.buddy_status)).setImageResource(
                StatusUtil.getStatusDrawable(
                        cursor.getString(COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE),
                        cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS)));
        String statusTitle = cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_TITLE);
        String statusMessage = cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_MESSAGE);
        ((TextView) view.findViewById(R.id.buddy_status_title)).setText(statusTitle);
        ((TextView) view.findViewById(R.id.buddy_status_message)).setText(statusMessage);
        // Unread counter.
        int unreadCount = cursor.getInt(COLUMN_ROSTER_BUDDY_UNREAD_COUNT);
        if(unreadCount > 0) {
            view.findViewById(R.id.counter_layout).setVisibility(View.VISIBLE);
            ((TextView)view.findViewById(R.id.counter_text)).setText(String.valueOf(unreadCount));
        } else {
            view.findViewById(R.id.counter_layout).setVisibility(View.GONE);
        }
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.alphabet_header, parent, false);
        }
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move mergeCursor to position " + position);
        }
        ((TextView) convertView.findViewById(R.id.header_text)).
                setText(String.valueOf(Character.toUpperCase((char) getCursor().getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX))));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return getCursor().getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        QueryBuilder queryBuilder = new QueryBuilder();
        switch (filter) {
            case FILTER_ONLINE_ONLY: {
                queryBuilder.columnNotEquals(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
                break;
            }
            case FILTER_ALL_BUDDIES:
            default:
        }
        if(!isShowTemp) {
            queryBuilder.and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        }
        queryBuilder.ascending(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX);
        return queryBuilder.createCursorLoader(context, Settings.BUDDY_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROSTER_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
        COLUMN_ROSTER_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
        COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
        COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
        COLUMN_ROSTER_BUDDY_STATUS_TITLE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE);
        COLUMN_ROSTER_BUDDY_STATUS_MESSAGE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE);
        COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE);
        COLUMN_ROSTER_BUDDY_ALPHABET_INDEX = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX);
        COLUMN_ROSTER_BUDDY_UNREAD_COUNT = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT);
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    public int getBuddyDbId(int position) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return getCursor().getInt(getCursor().getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }

    public void setRosterFilter(int filter) {
        this.filter = filter;
    }

    public int getRosterFilter() {
        return filter;
    }
}
