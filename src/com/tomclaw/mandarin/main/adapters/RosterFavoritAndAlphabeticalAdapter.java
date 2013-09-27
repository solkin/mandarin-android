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
import com.tomclaw.mandarin.util.MergeCursorLoader;
import com.tomclaw.mandarin.util.StatusUtil;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/27/13
 * Time: 10:05 PM
 */
public class RosterFavoritAndAlphabeticalAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<MergeCursor>, StickyListHeadersAdapter {

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

    public RosterFavoritAndAlphabeticalAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        // Initialize loader for dialogs Id.
        loaderManager.initLoader(ADAPTER_ALPHABETICAL_FAVORITE_ID, null, this);
    }

    @Override
    public Loader<MergeCursor> onCreateLoader(int id, Bundle bundle) {
        return new MergeCursorLoader(context);
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
        firstLetters = getFirstLetters(cursor);
    }

    @Override
    public void onLoaderReset(Loader<MergeCursor> cursorLoader) {
        swapCursor(null);
    }

    private String[] getFirstLetters(MergeCursor cursor){
        String[] firstLetters = new String[cursor.getCount()];
        favoritesSize = getFavoritesSize(cursor);
        cursor.moveToFirst();
        if (cursor.isAfterLast())
            return null;

        //firstLetters[0] = "Favorite";
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

    @Override
    public long getHeaderId(int position) {
        if (position < favoritesSize){
            return 0;
        }
        return firstLetters[position].charAt(0);
    }
}
