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
 * Created by solkin on 04.08.14.
 */
public class RosterSharingAdapter extends RosterStickyAdapter {


    private static final long GROUP_DIALOGS = 1;
    private static final long GROUP_CONTACTS = 2;

    public RosterSharingAdapter(Activity context, LoaderManager loaderManager) {
        super(context, loaderManager, FILTER_ALL_BUDDIES);
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
        TextView headerTextView = (TextView) convertView.findViewById(R.id.header_text);
        int headerTitle;
        boolean dialogOpened = cursor.getInt(COLUMN_ROSTER_BUDDY_DIALOG) == 1;
        if (dialogOpened) {
            headerTitle = R.string.dialogs;
        } else {
            headerTitle = R.string.buddies;
        }
        headerTextView.setText(getContext().getResources().getString(headerTitle));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        boolean dialogOpened = cursor.getInt(COLUMN_ROSTER_BUDDY_DIALOG) == 1;
        return dialogOpened ? GROUP_DIALOGS : GROUP_CONTACTS;
    }

    @Override
    protected void postQueryBuilder(QueryBuilder queryBuilder) {
        queryBuilder.descending(GlobalProvider.ROSTER_BUDDY_DIALOG).andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);

        /*queryBuilder.descending("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_DIALOG + " > 0 THEN "+GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME+" ELSE " + GlobalProvider.ROSTER_BUDDY_DIALOG + " END)").andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);*/
    }
}
