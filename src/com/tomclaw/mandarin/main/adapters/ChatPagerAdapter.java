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
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;

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

    public ChatPagerAdapter(Activity context, LoaderManager loaderManager) {
        this.context = context;
        this.loaderManager = loaderManager;
        inflater = context.getLayoutInflater();
        // Initialize loader for dialogs Id.
        this.loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View v = inflater.inflate(R.layout.chat_dialog, null);
        container.addView(v);
        return v;
    }

    @Override
    public int getCount() {
        if (cursor == null) {
            return 0;
        } else {
            return cursor.getCount();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getString(cursor.getColumnIndex(RosterProvider.ROSTER_BUDDY_NICK));
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
                Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
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
        notifyDataSetChanged();
    }
}