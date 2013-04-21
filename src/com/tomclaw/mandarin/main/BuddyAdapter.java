package com.tomclaw.mandarin.main;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
public class BuddyAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Context context;
    private List<AccountRoot> accountRoots;

    public BuddyAdapter(Context context, List<AccountRoot> accountRoots) {
        this.accountRoots = accountRoots;
        this.context = context;
        inflater = ((Activity) context).getLayoutInflater();
    }

    @Override
    public int getCount() {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            for (GroupItem groupItem : roster.getGroupItems()) {
                count += groupItem.getItems().size();
            }
        }
        return count;
    }

    @Override
    public BuddyItem getItem(int position) {
        int count = 0;
        for (AccountRoot accountRoot : accountRoots) {
            Roster roster = accountRoot.getRoster();
            for (GroupItem groupItem : roster.getGroupItems()) {
                List<BuddyItem> buddyItems = groupItem.getItems();
                if (count + buddyItems.size() > position) {
                    return buddyItems.get(position-count);
                }
                count += buddyItems.size();
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BuddyItem buddyItem = getItem(position);
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
}
