package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 17:44
 */
public class RosterStatusAdapter extends RosterStickyAdapter {

    public RosterStatusAdapter(Activity context, LoaderManager loaderManager, int filter) {
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
        boolean isOnline = cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS) != StatusUtil.STATUS_OFFLINE;
        String headerText = getContext().getString(isOnline ? R.string.status_online : R.string.status_offline);
        ((TextView) convertView.findViewById(R.id.header_text)).setText(headerText.toUpperCase());
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS) != StatusUtil.STATUS_OFFLINE ? 1 : 0;
    }

    @Override
    protected void postQueryBuilder(QueryBuilder queryBuilder) {
        queryBuilder.descending("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_STATUS + " != " +
                StatusUtil.STATUS_OFFLINE + " THEN 1 ELSE 0 END)").andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);
    }
}
