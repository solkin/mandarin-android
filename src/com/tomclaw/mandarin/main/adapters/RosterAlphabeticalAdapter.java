package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
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
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class RosterAlphabeticalAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor>, StickyListHeadersAdapter, BuddyDbIdGetter {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_ALPHABETICAL_ID = -6;
    private static final char UNCLASSIFIED = ' ';

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

    public RosterAlphabeticalAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        // Initialize loader for dialogs Id.
        loaderManager.initLoader(ADAPTER_ALPHABETICAL_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context, Settings.BUDDY_RESOLVER_URI, null, null, null,
                GlobalProvider.ROSTER_BUDDY_NICK + " COLLATE NOCASE ASC");
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

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.buddy_item_divider, parent, false);
        }
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move mergeCursor to position " + position);
        }
        ((TextView) convertView.findViewById(R.id.divider_text)).
                setText(getFirstLetter(getCursor().getString(COLUMN_ROSTER_BUDDY_NICK)));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move mergeCursor to position " + position);
        }
        return getFirstChar(getCursor().getString(COLUMN_ROSTER_BUDDY_NICK));
    }

    @Override
    public int getBuddyDbId(int position) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move mergeCursor to position " + position);
        }
        return getCursor().getInt(getCursor().getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }

    public String getFirstLetter(String word) {
        return String.valueOf(getFirstChar(word));
    }

    private static char getFirstChar(String word) {
        char letter = word.charAt(0);
        if (Character.isLetter(letter)) {
            return Character.toUpperCase(letter);
        }
        return UNCLASSIFIED;
    }
}

