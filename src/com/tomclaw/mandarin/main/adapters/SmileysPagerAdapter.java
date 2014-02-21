package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.SmileyParser;

/**
 * Created by solkin on 16/02/14.
 */
public class SmileysPagerAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater inflater;
    private SmileyParser smileyParser;
    private int smileysPerPage;
    private int smileysSize;
    private int columnCount;

    public SmileysPagerAdapter(Activity context, int width, int height) {
        this.context = context;
        this.inflater = context.getLayoutInflater();
        smileyParser = SmileyParser.getInstance();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        // TODO: maybe use dimensions xml?
        smileysSize = (int) context.getResources().getDimension(R.dimen.smiley_size);// (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, displayMetrics);
        setPagerSize(width, height);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = inflater.inflate(R.layout.smileys_grid, null);
        GridView gridView = (GridView) view.findViewById(R.id.smileys_grid);
        // This will determine horizontal smileys count.
        gridView.setColumnWidth(smileysSize);
        gridView.setNumColumns(columnCount);
        // Create special smileys adapter to show on the grid.
        SmileysGridAdapter gridAdapter = new SmileysGridAdapter(context, inflater, position, smileysPerPage);
        gridView.setAdapter(gridAdapter);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        if(smileysPerPage == 0) {
            return 0;
        }
        return 1 + smileyParser.getSmileysCount() / smileysPerPage;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    private void setPagerSize(int width, int height) {
        smileysPerPage = (width * height) / (smileysSize * smileysSize);
        columnCount = width / smileysSize;
    }
}
