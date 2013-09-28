package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.MergeCursor;
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
import com.tomclaw.mandarin.util.MergeCursorLoader;
import com.tomclaw.mandarin.util.QueryParametersContainer;
import com.tomclaw.mandarin.util.StatusUtil;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class RosterFavoriteAndAlphabeticalAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<MergeCursor>, StickyListHeadersAdapter, BuddyDbIdGetter {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_ALPHABETICAL_FAVORITE_ID = -6;

    /**
     * Columns
     */
    private static int COLUMN_ROSTER_BUDDY_ID;
    private static int COLUMN_ROSTER_BUDDY_NICK;
    private static int COLUMN_ROSTER_BUDDY_STATUS;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE;
    private static int COLUMN_ROSTER_BUDDY_FAVORITE;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;
    private String[] firstLetters;
    private int favoritesSize;
    private MergeCursor mergeCursor;

    public RosterFavoriteAndAlphabeticalAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        // Initialize loader for dialogs Id.
        loaderManager.initLoader(ADAPTER_ALPHABETICAL_FAVORITE_ID, null, this);
    }

    @Override
    public Loader<MergeCursor> onCreateLoader(int id, Bundle bundle) {
        QueryParametersContainer[] containers = new QueryParametersContainer[2];
        containers[0] = new QueryParametersContainer(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_FAVORITE + "='" + 1 + "'",null, "(CASE WHEN " +
                GlobalProvider.ROSTER_BUDDY_STATUS + "=" + StatusUtil.STATUS_OFFLINE+
                " THEN 0 ELSE 1 END" + ") DESC," + GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
        containers[1] = new QueryParametersContainer(Settings.BUDDY_RESOLVER_URI, null, null, null,
                GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
        return new MergeCursorLoader(context, containers);
    }

    @Override
    public void onLoadFinished(Loader<MergeCursor> cursorLoader, MergeCursor cursor) {
        // Detecting columns.
        COLUMN_ROSTER_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
        COLUMN_ROSTER_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
        COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
        COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE);
        COLUMN_ROSTER_BUDDY_FAVORITE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_FAVORITE);
        swapCursor(cursor);
        this.mergeCursor = cursor;
        firstLetters = getFirstLetters(cursor);
    }

    @Override
    public void onLoaderReset(Loader<MergeCursor> cursorLoader) {
        swapCursor(null);
        this.mergeCursor = null;
    }

    private String[] getFirstLetters(MergeCursor cursor){
        String[] firstLetters = new String[cursor.getCount()];
        favoritesSize = getFavoritesSize(cursor);
        cursor.moveToFirst();
        if (cursor.isAfterLast())
            return null;

        int i = 0;
        while (i < favoritesSize){
            firstLetters[i++] = "Favorite";
            cursor.moveToNext();
        }
        do {
            firstLetters[i++] = cursor.getString(COLUMN_ROSTER_BUDDY_NICK).substring(0, 1);
        }
        while (cursor.moveToNext());
        return firstLetters;
    }

    /**
     * Return number of favorite contacts
     */
    private int getFavoritesSize(MergeCursor cursor){
        int count = 0;
        cursor.moveToFirst();
        do {
            if (cursor.getInt(COLUMN_ROSTER_BUDDY_FAVORITE) == 1)
                count++;
        } while (cursor.moveToNext());
        cursor.moveToFirst();
        return count/2;
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
        ((TextView) convertView.findViewById(R.id.divider_text)).setText(firstLetters[position]);
        return convertView;
    }

    /**
     * Return 0 for items from "Favorite" section. Return first letter for items from alphabetical list
     */
    @Override
    public long getHeaderId(int position) {
        if (position < favoritesSize){
            return 0;
        }
        return firstLetters[position].charAt(0);
    }

    @Override
    public int getBuddyDbId(int position){
        if (!mergeCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move mergeCursor to position " + position);
        }
        return mergeCursor.getInt(getCursor().getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }
}
