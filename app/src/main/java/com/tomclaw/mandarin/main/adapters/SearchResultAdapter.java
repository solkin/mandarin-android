package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.design.ContactBadge;
import com.tomclaw.helpers.TimeHelper;

/**
 * Created by Solkin on 09.07.2014.
 */
public class SearchResultAdapter extends EndlessListAdapter<ShortBuddyInfo> {

    public SearchResultAdapter(Context context, EndlessAdapterListener listener) {
        super(context, listener);
    }

    @Override
    protected int getItemLayout() {
        return R.layout.search_result_item;
    }

    @Override
    public void bindView(View view, Context context, ShortBuddyInfo info) {
        TextView buddyNick = view.findViewById(R.id.buddy_nick);
        TextView buddyShortInfo = view.findViewById(R.id.buddy_short_info);
        TextView onlineIndicator = view.findViewById(R.id.online_indicator);
        // Buddy friendly name - first and last names or nick name.
        String friendly = "";
        if (Strings.isEmptyOrWhitespace(info.getFirstName()) && Strings.isEmptyOrWhitespace(info.getLastName())) {
            if (Strings.isEmptyOrWhitespace(info.getBuddyNick())) {
                friendly = info.getBuddyId();
            } else {
                friendly = info.getBuddyNick();
            }
        } else {
            friendly = Strings.appendIfNotEmpty(friendly, info.getFirstName(), "");
            friendly = Strings.appendIfNotEmpty(friendly, info.getLastName(), " ");
        }
        buddyNick.setText(friendly);
        // Buddy home address.
        String shortInfo = Strings.appendIfNotEmpty("", info.getHomeAddress(), "");
        // Buddy gender. Female, male or nothing.
        String gender;
        switch (info.getGender()) {
            case Female:
                gender = context.getString(R.string.female);
                break;
            case Male:
                gender = context.getString(R.string.male);
                break;
            default:
                gender = "";
                break;
        }
        shortInfo = Strings.appendIfNotEmpty(shortInfo, gender, ", ");
        // Buddy years.
        String birthDate = "";
        if (info.getBirthDate() > 0) {
            int years = TimeHelper.getYears(info.getBirthDate());
            if (years > 0) {
                birthDate = context.getResources().getQuantityString(R.plurals.buddy_years, years, years);
            }
        }
        shortInfo = Strings.appendIfNotEmpty(shortInfo, birthDate, ", ");

        buddyShortInfo.setText(shortInfo);
        onlineIndicator.setText(info.isOnline() ? R.string.status_online : R.string.status_offline);
        onlineIndicator.setBackgroundResource(info.isOnline() ? R.drawable.green_indicator : R.drawable.red_indicator);
        // Avatar.
        ContactBadge contactBadge = view.findViewById(R.id.buddy_image);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, info.getAvatarHash(), R.drawable.def_avatar_x48, false);
    }
}
