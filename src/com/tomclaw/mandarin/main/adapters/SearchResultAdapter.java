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
public class SearchResultAdapter extends EndlessListAdapter<SearchBuddyInfo> {

    public SearchResultAdapter(Context context, EndlessAdapterListener listener) {
        super(context, listener);
    }

    @Override
    protected int getItemLayout() {
        return R.layout.search_result_item;
    }

    @Override
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
            int years = TimeHelper.getYears(info.getBirthDate());
            if(years > 0) {
                birthDate = context.getResources().getQuantityString(R.plurals.buddy_years, years, years);
            }
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
}
