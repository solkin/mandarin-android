package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.HistorySelection;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/6/13
 * Time: 8:51 PM
 */
public class ChatPagerAdapter extends PagerAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ADAPTER_DIALOGS_ID = -3;

    private Activity activity;
    private LoaderManager loaderManager;
    private Cursor cursor;
    private LayoutInflater inflater;
    private Runnable onUpdate;
    private Runnable onLongClick;

    public ChatPagerAdapter(Activity activity, LoaderManager loaderManager,
                            Runnable onUpdate, Runnable onLongClick) {
        super();
        this.activity = activity;
        this.loaderManager = loaderManager;
        this.onUpdate = onUpdate;
        this.onLongClick = onLongClick;
        inflater = activity.getLayoutInflater();
        // Initialize loader for dialogs Id.
        this.loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log.d(Settings.LOG_TAG, "instantiateItem for position = " + position);
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View view = inflater.inflate(R.layout.chat_dialog, null);
        ListView chatList = (ListView) view.findViewById(R.id.chat_list);
        final ChatHistoryAdapter chatHistoryAdapter = new ChatHistoryAdapter(activity, loaderManager,
                cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID)));
        chatList.setAdapter(chatHistoryAdapter);
        // Long-click listener to activate action mode and show check-boxes.
        AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Checking for action mode is already activated.
                if (HistorySelection.getInstance().getSelectionMode()) {
                    // Hm. Action mode is already active.
                    return false;
                }
                // Update selection data.
                HistorySelection.getInstance().setSelectionMode(true);
                HistorySelection.getInstance().setHistoryAdapter(chatHistoryAdapter);
                // Update history adapter to show checkboxes.
                HistorySelection.getInstance().notifyHistoryAdapter();
                // Callback activity for action mode is actual.
                onLongClick.run();
                return true;
            }
        };
        chatList.setOnItemLongClickListener(itemLongClickListener);
        // Click listener for item clicked events (in selection mode).
        AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(Settings.LOG_TAG, "clicked: " + position + " id: " + id);
                String itemText;
                boolean selectionExist = HistorySelection.getInstance().isSelectionExist(position);
                if(selectionExist) {
                    itemText = null;
                } else {
                    itemText = chatHistoryAdapter.getItemText(position);
                }
                HistorySelection.getInstance().setSelection(position, itemText);
                chatHistoryAdapter.notifyDataSetChanged();
            }
        };
        chatList.setOnItemClickListener(itemClickListener);
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
        return cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK));
    }

    public int getPageBuddyDbId(int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }

    public int getPageAccountDbId(int position) {
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
    }

    public int getPagePosition(int buddyDbId) {
        if (cursor != null) {
            // Does this code Ok? I'm not sure.
            for (int c = 0; c < cursor.getCount(); c++) {
                // Trying to move row.
                if (cursor.moveToPosition(c)) {
                    // Checking for buddy db id equals.
                    if (cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID)) == buddyDbId) {
                        return c;
                    }
                }
            }
        }
        // This should never happen.
        return 0;
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
        return new CursorLoader(activity,
                Settings.BUDDY_RESOLVER_URI, null, GlobalProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, "(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_STATUS + "=" + StatusUtil.STATUS_OFFLINE
                + " THEN 0 ELSE 1 END" + ") DESC," + GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
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
        // Before notifying UI about update, run special event.
        onUpdate.run();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}