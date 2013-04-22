package com.tomclaw.mandarin.main;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.BuddyItem;
import com.tomclaw.mandarin.im.GroupItem;
import com.tomclaw.mandarin.im.Roster;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class BuddyAdapter extends BaseExpandableListAdapter {

    private LayoutInflater inflater;
    private Context context;
    private List<AccountRoot> accountRoots;

    public BuddyAdapter(Context context, List<AccountRoot> accountRoots) {
        this.accountRoots = accountRoots;
        this.context = context;
        inflater = ((Activity) context).getLayoutInflater();
    }

    @Override
    public int getGroupCount() {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            count += roster.getGroupItems().size();
        }
        return count;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            if(groupPosition >= count && count + roster.getGroupItems().size() > groupPosition) {
                return roster.getGroupItems().get(groupPosition - count).getItems().size();
            }
            count += roster.getGroupItems().size();
        }
        return count;
    }

    @Override
    public GroupItem getGroup(int groupPosition) {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            if(groupPosition >= count && count + roster.getGroupItems().size() > groupPosition) {
                return roster.getGroupItems().get(groupPosition - count);
            }
            count += roster.getGroupItems().size();
        }
        return null;
    }

    @Override
    public BuddyItem getChild(int groupPosition, int childPosition) {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            if(groupPosition >= count && count + roster.getGroupItems().size() > groupPosition) {
                return roster.getGroupItems().get(groupPosition - count).getItems().get(childPosition);
            }
            count += roster.getGroupItems().size();
        }
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupItem groupItem = getGroup(groupPosition);
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.group_item, parent, false);
        }
        // Setup text values
        ((TextView) view.findViewById(R.id.groupName)).setText(groupItem.getGroupName());
        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        BuddyItem buddyItem = getChild(groupPosition, childPosition);
        // Obtain view
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.buddy_item, parent, false);
        }
        // Setup text values
        ((TextView) view.findViewById(R.id.buddyId)).setText(buddyItem.getBuddyId());
        ((TextView) view.findViewById(R.id.buddyNick)).setText(buddyItem.getBuddyNick());
        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
