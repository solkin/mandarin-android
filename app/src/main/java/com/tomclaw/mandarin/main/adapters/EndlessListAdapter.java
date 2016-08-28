package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.tomclaw.mandarin.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Solkin on 10.07.2014.
 */
public abstract class EndlessListAdapter<T> extends BaseAdapter {

    private final List<T> tList = new ArrayList<T>();
    private boolean isMoreItemsAvailable = false;
    private EndlessAdapterListener listener;

    private int staticItemsCount = 0;
    private int latestInvokedPosition = -1;

    private Context context;
    private LayoutInflater inflater;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_PROGRESS = 1;

    public EndlessListAdapter(Context context, EndlessAdapterListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return tList.size() + (isMoreItemsAvailable ? TYPE_PROGRESS : TYPE_NORMAL);
    }

    @Override
    public T getItem(int position) {
        if (isMoreItemsAvailable && position == tList.size()) {
            return null;
        }
        return tList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return (isMoreItemsAvailable && position == tList.size()) ? TYPE_PROGRESS : TYPE_NORMAL;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            if (convertView == null) {
                view = newView(position, parent);
            } else {
                view = convertView;
            }
            T t = getItem(position);
            if (getItemViewType(position) == TYPE_NORMAL) {
                bindView(view, context, t);
            } else {
                onShowProgressItem(position);
            }
        } catch (Throwable ex) {
            view = newView(position, parent);
        }
        return view;
    }

    protected abstract int getItemLayout();

    public View newView(int position, ViewGroup viewGroup) {
        int layoutRes;
        if (getItemViewType(position) == TYPE_NORMAL) {
            layoutRes = getItemLayout();
        } else {
            layoutRes = R.layout.endless_list_wait_item;
        }
        return inflater.inflate(layoutRes, viewGroup, false);
    }

    public abstract void bindView(View view, Context context, T t);

    public void appendStaticItem(T t) {
        tList.add(staticItemsCount++, t);
    }

    public void appendItem(T t) {
        tList.add(t);
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(tList);
    }

    public void setMoreItemsAvailable(boolean isMoreItemsAvailable) {
        this.isMoreItemsAvailable = isMoreItemsAvailable;
    }

    private void onShowProgressItem(int position) {
        if (position > latestInvokedPosition) {
            listener.onLoadMoreItems(tList.size() - staticItemsCount);
            latestInvokedPosition = position;
        }
    }

    public boolean isEmpty() {
        return tList.isEmpty();
    }

    public int getStaticItemsCount() {
        return staticItemsCount;
    }

    public interface EndlessAdapterListener {
        public void onLoadMoreItems(int offset);
    }
}
