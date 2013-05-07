package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DataProvider;
import com.tomclaw.mandarin.core.Settings;
import com.viewpageindicator.PageIndicator;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/6/13
 * Time: 8:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatPagerAdapter extends PagerAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ADAPTER_DIALOGS_ID = -3;

    private Context context;
    private LoaderManager loaderManager;
    private Cursor cursor;
    private LayoutInflater inflater;
    private PageIndicator indicator;

    public ChatPagerAdapter(Activity context, LoaderManager loaderManager, PageIndicator indicator) {
        super();
        this.context = context;
        this.loaderManager = loaderManager;
        this.indicator = indicator;
        inflater = context.getLayoutInflater();
        // Initialize loader for dialogs Id.
        this.loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View view = inflater.inflate(R.layout.chat_dialog, null);
        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        // While there is no cursor, we should not show anything.
        return (cursor == null) ? 0 : cursor.getCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getString(cursor.getColumnIndex(DataProvider.ROSTER_BUDDY_NICK));
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(context,
                Settings.BUDDY_RESOLVER_URI, null, DataProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, DataProvider.ROSTER_BUDDY_STATE + " DESC," + DataProvider.ROSTER_BUDDY_NICK + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        swapCursor(null);
    }

    public void swapCursor(Cursor cursor) {
        if (this.cursor == cursor) {
            return;
        }
        this.cursor = cursor;
        // Notify page indicator and base adapter data was changed.
        notifyDataSetChanged();
        indicator.notifyDataSetChanged();
    }
}