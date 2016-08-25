package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.im.BuddyCursor;
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
        groupsMap = new HashMap<>();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        BuddyCursor cursor = getBuddyCursor();
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.roster_sticky_header, parent, false);
        }
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        ((TextView) convertView.findViewById(R.id.header_text))
                .setText(cursor.getBuddyGroup().toUpperCase());
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        BuddyCursor cursor = getBuddyCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        String groupName = cursor.getBuddyGroup();
        Integer groupId = groupsMap.get(groupName);
        if (groupId == null) {
            groupId = groupsMap.size();
            groupsMap.put(groupName, groupId);
        }
        return groupId;
    }

    @Override
    protected void postQueryBuilder(QueryBuilder queryBuilder) {
        queryBuilder.ascending(GlobalProvider.ROSTER_BUDDY_GROUP).andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX).andOrder()
                .ascending(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);
    }
}
