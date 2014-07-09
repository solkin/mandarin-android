package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.SearchBuddyInfo;
import com.tomclaw.mandarin.util.TimeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Solkin on 09.07.2014.
 */
public class SearchResultAdapter extends BaseAdapter {

    private final List<SearchBuddyInfo> infoList = new ArrayList<SearchBuddyInfo>();
    private TimeHelper timeHelper;
    private boolean isMoreItemsAvailable = false;

    private Context context;
    private LayoutInflater inflater;

    public SearchResultAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.timeHelper = new TimeHelper(context);
    }

    @Override
    public int getCount() {
        return infoList.size() + (isMoreItemsAvailable ? 1 : 0);
    }

    @Override
    public SearchBuddyInfo getItem(int position) {
        if(isMoreItemsAvailable && position == infoList.size()) {
            return null;
        }
        return infoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (isMoreItemsAvailable && position == infoList.size()) ? 1 : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            if (convertView == null) {
                view = newView(context, parent);
            } else {
                view = convertView;
            }
            SearchBuddyInfo info = getItem(position);
            bindView(view, context, info);
        } catch (Throwable ex) {
            view = inflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        return view;
    }

    public View newView(Context context, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.search_result_item, viewGroup, false);
    }

    public void bindView(View view, Context context, SearchBuddyInfo info) {
        TextView buddyNick = (TextView) view.findViewById(R.id.buddy_nick);
        TextView buddyShortInfo = (TextView) view.findViewById(R.id.buddy_short_info);
        TextView onlineIndicator = (TextView) view.findViewById(R.id.online_indicator);
        // Buddy friendly name - first and last names or nick name.
        String friendly = "";
        if(TextUtils.isEmpty(info.getFirstName()) && TextUtils.isEmpty(info.getLastName())) {
            if(TextUtils.isEmpty(info.getBuddyNick())) {
                friendly = info.getBuddyId();
            } else {
                friendly = info.getBuddyNick();
            }
        } else {
            friendly = appendIfNotEmpty(friendly, info.getFirstName(), "");
            friendly = appendIfNotEmpty(friendly, info.getLastName(), " ");
        }
        buddyNick.setText(friendly);
        // Buddy home address.
        String shortInfo = appendIfNotEmpty("", info.getHomeAddress(), "");
        // Buddy gender. Female, male or nothing.
        String gender;
        switch(info.getGender()) {
            case Female:
                gender = context.getString(R.string.female);
                break;
            case Male:
                gender = context.getString(R.string.female);
                break;
            default:
                gender = "";
                break;
        }
        shortInfo = appendIfNotEmpty(shortInfo, gender, ", ");
        // Buddy years.
        String birthDate = "";
        if(info.getBirthDate() > 0) {
            birthDate = timeHelper.getFormattedYears(System.currentTimeMillis() - info.getBirthDate());
        }
        shortInfo = appendIfNotEmpty(shortInfo, birthDate, ", ");

        buddyShortInfo.setText(shortInfo);
        onlineIndicator.setText(info.isOnline() ? R.string.status_online : R.string.status_offline);
        onlineIndicator.setBackgroundResource(info.isOnline() ? R.drawable.green_indicator : R.drawable.red_indicator);
    }

    private String appendIfNotEmpty(String where, String what, String divider) {
        if(!TextUtils.isEmpty(what)) {
            if (!TextUtils.isEmpty(where)) {
                where += divider;
            }
            where += what;
        }
        return where;
    }

    public void appendResult(SearchBuddyInfo info) {
        infoList.add(info);
    }

    public void setMoreItemsAvailable(boolean isMoreItemsAvailable) {
        this.isMoreItemsAvailable = isMoreItemsAvailable;
    }
}
