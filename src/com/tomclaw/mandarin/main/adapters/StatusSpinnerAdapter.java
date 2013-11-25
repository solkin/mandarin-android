package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 20.10.13
 * Time: 15:40
 */
public class StatusSpinnerAdapter extends ArrayAdapter<Integer> {

    private final LayoutInflater inflater;
    private final String accountType;

    private static final int DROPDOWN_PADDING = 10;

    public StatusSpinnerAdapter(Activity context, String accountType) {
        super(context, R.layout.status_item, StatusUtil.getConnectStatuses(accountType));

        inflater = context.getLayoutInflater();
        this.accountType = accountType;
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
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.status_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
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
}
