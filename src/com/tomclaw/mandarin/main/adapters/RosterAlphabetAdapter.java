package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 17:44
 */
public class RosterAlphabetAdapter extends RosterStickyAdapter {

    public RosterAlphabetAdapter(Activity context, LoaderManager loaderManager, int filter) {
        super(context, loaderManager, filter);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.roster_sticky_header, parent, false);
        }
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        ((TextView) convertView.findViewById(R.id.header_text)).setText(String.valueOf(
                Character.toUpperCase((char) cursor.getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX))));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX);
    }

    @Override
    protected void postQueryBuilder(QueryBuilder queryBuilder) {
        queryBuilder.ascending(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX);
    }
}
