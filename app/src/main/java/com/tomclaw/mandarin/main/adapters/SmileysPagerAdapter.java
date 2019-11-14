package com.tomclaw.mandarin.main.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.OnSmileyClickCallback;
import com.tomclaw.mandarin.util.SmileyParser;

/**
 * Created by solkin on 16/02/14.
 */
public class SmileysPagerAdapter extends PagerAdapter {

    private LayoutInflater inflater;
    private SmileyParser smileyParser;
    private int smileysPerPage;
    private int smileySize;
    private int columnCount;
    private int rowCount;
    private OnSmileyClickCallback callback;

    public SmileysPagerAdapter(Activity context, int width, int height, OnSmileyClickCallback callback) {
        this.inflater = LayoutInflater.from(context);
        smileyParser = SmileyParser.getInstance();
        smileySize = (int) context.getResources().getDimension(R.dimen.smiley_size);
        int indicator_height = (int) context.getResources().getDimension(R.dimen.indicator_height);
        setPagerSize(width, height - indicator_height);
        this.callback = callback;
    }

    @Override
    @NonNull
    @SuppressLint("InflateParams")
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = inflater.inflate(R.layout.smileys_grid, null);
        GridLayout gridLayout = view.findViewById(R.id.smileys_grid);
        gridLayout.setColumnCount(columnCount);
        gridLayout.setRowCount(rowCount);
        for (int row = 0; row < rowCount; row++) {
            int pageColumnCount = columnCount;
            int smilesAdded = position * smileysPerPage + row * columnCount;
            if (smilesAdded + columnCount > smileyParser.getSmileysCount()) {
                pageColumnCount = smileyParser.getSmileysCount() - smilesAdded;
            }
            for (int column = 0; column < pageColumnCount; column++) {
                View itemView = inflater.inflate(R.layout.smiley_item, null);
                ImageView imageView = itemView.findViewById(R.id.smiley_image);
                imageView.setImageResource(smileyParser.getSmiley(smilesAdded + column));
                final String smileyText = smileyParser.getSmileyText(smilesAdded + column);
                itemView.setOnClickListener(v -> callback.onSmileyClick(smileyText));
                GridLayout.LayoutParams itemLayoutParams = new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column));
                itemLayoutParams.width = smileySize;
                itemLayoutParams.height = smileySize;
                gridLayout.addView(itemView, itemLayoutParams);
            }
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        if (smileysPerPage == 0) {
            return 0;
        }
        return 1 + smileyParser.getSmileysCount() / smileysPerPage;
    }

    @Override
    public boolean isViewFromObject(View view, @NonNull Object object) {
        return view.equals(object);
    }

    private void setPagerSize(int width, int height) {
        columnCount = width / smileySize;
        rowCount = height / smileySize;
        smileysPerPage = columnCount * rowCount;
    }
}
