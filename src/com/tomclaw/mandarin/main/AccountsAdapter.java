package com.tomclaw.mandarin.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountsAdapter extends ArrayAdapter<AccountRoot> {

    private LayoutInflater inflater;
    private Context context;

    public AccountsAdapter(Context context, int textViewResourceId, List objects) {
        super(context, textViewResourceId, objects);
        this.context = context;
        inflater = ((Activity)context).getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AccountRoot accountRoot = getItem(position);
        // Obtain view
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.account_item, parent, false);
        }
        // Setup text values
        ((TextView) view.findViewById(R.id.userId)).setText(accountRoot.getUserId());
        ((TextView) view.findViewById(R.id.userNick)).setText(accountRoot.getUserNick());
         // Creating listeners for status click
        view.findViewById(R.id.userStatus).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.startActivity(new Intent(context, StatusActitvity.class));
                    }
                });
        return view;
    }
}
