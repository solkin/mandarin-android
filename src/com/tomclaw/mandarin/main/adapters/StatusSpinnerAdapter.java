package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 20.10.13
 * Time: 15:40
 */
public class StatusSpinnerAdapter extends ArrayAdapter<Integer> {

    private final LayoutInflater inflater;
    private final String accountType;
    private final List<Integer> statusList;

    private static final int DROPDOWN_PADDING = 10;

    public StatusSpinnerAdapter(Context context, String accountType, List<Integer> statusList) {
        super(context, R.layout.status_item, statusList);
        inflater = LayoutInflater.from(context);
        this.accountType = accountType;
        this.statusList = statusList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            int statusIndex = getItem(position);
            if (convertView == null) {
                view = newView(parent);
            } else {
                view = convertView;
            }
            bindView(view, statusIndex);
        } catch (Throwable ex) {
            view = newView(parent);
            Logger.log("exception in getView: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = getView(position, convertView, parent);
        view.setPadding(DROPDOWN_PADDING, DROPDOWN_PADDING, DROPDOWN_PADDING, DROPDOWN_PADDING);
        return view;
    }

    public View newView(ViewGroup viewGroup) {
        return inflater.inflate(R.layout.status_item, viewGroup, false);
    }

    private void bindView(View view, int statusIndex) {
        ImageView statusImage = (ImageView) view.findViewById(R.id.status_icon);
        TextView statusName = (TextView) view.findViewById(R.id.status_name);

        statusImage.setImageResource(StatusUtil.getStatusDrawable(accountType, statusIndex));
        statusName.setText(StatusUtil.getStatusTitle(accountType, statusIndex));
    }

    public int getStatus(int position) {
        return getItem(position);
    }

    public int getStatusPosition(int statusValue) throws StatusNotFoundException {
        int statusPosition = Collections.binarySearch(statusList, statusValue);
        if (statusPosition < 0) {
            throw new StatusNotFoundException();
        }
        return statusPosition;
    }
}
