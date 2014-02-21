package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.SmileyParser;

/**
 * Created by solkin on 17/02/14.
 */
public class SmileysGridAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private int page, smileysPerPage;

    public SmileysGridAdapter(Context context, LayoutInflater inflater, int page, int smileysPerPage) {
        this.context = context;
        this.inflater = inflater;
        this.page = page;
        this.smileysPerPage = smileysPerPage;
    }

    @Override
    public int getCount() {
        int smileysCount = SmileyParser.getInstance().getSmileysCount();
        int firstSmile = page * smileysPerPage;

        if(smileysCount - firstSmile < smileysPerPage) {
            return smileysCount - firstSmile;
        }
        return smileysPerPage;
    }

    @Override
    public Object getItem(int position) {
        return getItemId(position);
    }

    @Override
    public long getItemId(int position) {
        return SmileyParser.getInstance().getSmiley(page * smileysPerPage + position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.smiley_item, null);
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.smiley_image);
        imageView.setImageResource(((Long) getItem(position)).intValue());
        return view;
    }
}
