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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by solkin on 04.08.14.
 */
public class RosterGroupAdapter extends RosterStickyAdapter {

    private transient Map<String, Integer> groupsMap;

    public RosterGroupAdapter(Activity context, LoaderManager loaderManager, int filter) {
        super(context, loaderManager, filter);
        groupsMap = new HashMap<String, Integer>();
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
        ((TextView) convertView.findViewById(R.id.header_text))
                .setText(String.valueOf(cursor.getString(COLUMN_ROSTER_BUDDY_GROUP).toUpperCase()));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        String groupName = cursor.getString(COLUMN_ROSTER_BUDDY_GROUP);
        Integer groupId = groupsMap.get(groupName);
        if(groupId == null) {
            groupId = groupsMap.size();
            groupsMap.put(groupName, groupId);
        }
        return groupId;
    }

    @Override
    protected void postQueryBuilder(QueryBuilder queryBuilder) {
        queryBuilder.ascending(GlobalProvider.ROSTER_BUDDY_GROUP).andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);
    }
}
