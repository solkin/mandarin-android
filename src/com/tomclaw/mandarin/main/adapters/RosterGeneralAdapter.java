package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorTreeAdapter;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class RosterGeneralAdapter extends SimpleCursorTreeAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ADAPTER_GENERAL_ID = -1;

    private static final String groupFrom[] = {GlobalProvider.ROSTER_GROUP_NAME};
    private static final int groupTo[] = {R.id.group_name};

    private static final String childFrom[] = {GlobalProvider.ROSTER_BUDDY_ID, GlobalProvider.ROSTER_BUDDY_NICK,
            GlobalProvider.ROSTER_BUDDY_STATUS};
    private static final int childTo[] = {R.id.buddy_id, R.id.buddy_nick, R.id.buddy_status};

    private Context context;
    private LoaderManager loaderManager;

    public RosterGeneralAdapter(Context context, LoaderManager loaderManager) {
        super(context, null, R.layout.group_item, R.layout.group_item, groupFrom, groupTo,
                R.layout.buddy_item, R.layout.buddy_item, childFrom, childTo);
        this.context = context;
        this.loaderManager = loaderManager;
        // Initialize loader for groups.
        this.loaderManager.initLoader(ADAPTER_GENERAL_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Checking general Id. This may be group or its content.
        if (id == ADAPTER_GENERAL_ID) {
            return new CursorLoader(context, Settings.GROUP_RESOLVER_URI, null, null,
                    null, GlobalProvider.ROSTER_GROUP_NAME + " ASC");
        } else {
            return new CursorLoader(context, Settings.BUDDY_RESOLVER_URI, null, GlobalProvider.ROSTER_BUDDY_GROUP
                    + "='" + bundle.getString(GlobalProvider.ROSTER_BUDDY_GROUP) + "'", null,
                    GlobalProvider.ROSTER_BUDDY_STATE + " DESC," + GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == ADAPTER_GENERAL_ID) {
            setGroupCursor(cursor);
        } else {
            try {
                setChildrenCursor(cursorLoader.getId(), cursor);
            } catch (Throwable ex) {
                // Nothing to do in this case.
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == ADAPTER_GENERAL_ID) {
            setGroupCursor(null);
        } else {
            try {
                setChildrenCursor(cursorLoader.getId(), null);
            } catch (Throwable ex) {
                // Nothing to do in this case.
            }
        }
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // This will calls when collapsed group expands.
        int groupPosition = groupCursor.getPosition();
        int columnIndex = groupCursor.getColumnIndex(GlobalProvider.ROSTER_GROUP_NAME);
        String groupName = groupCursor.getString(columnIndex);
        Log.d(Settings.LOG_TAG, "Child cursor for " + groupName + "(" + groupPosition + ") loading started");
        // Store group name into bundle to have opportunity build query.
        Bundle bundle = new Bundle();
        bundle.putString(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
        // Check for loader already started.
        if (loaderManager.getLoader(groupPosition) != null
                && !loaderManager.getLoader(groupPosition).isReset()) {
            loaderManager.restartLoader(groupPosition, bundle, this);
        } else {
            loaderManager.initLoader(groupPosition, bundle, this);
        }
        // Returns null. Sorry, but we have no child cursor this time.
        return null;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        View view;
        try {
            Cursor cursor = getGroup(groupPosition);
            if (cursor == null) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (convertView == null) {
                view = newGroupView(context, cursor, isExpanded, parent);
            } else {
                view = convertView;
            }
            bindGroupView(view, context, cursor, isExpanded);
        } catch (Throwable ex) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.group_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in roster general adapter: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View view;
        try {
            Cursor cursor = getChild(groupPosition, childPosition);
            if (cursor == null) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (convertView == null) {
                view = newChildView(context, cursor, isLastChild, parent);
            } else {
                view = convertView;
            }
            bindChildView(view, context, cursor, isLastChild);
        } catch (Throwable ex) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in roster general adapter: " + ex.getMessage());
        }
        return view;
    }

    public long getBuddyDbId(int groupPosition, int childPosition) {
        Cursor cursor = getChild(groupPosition, childPosition);
        return cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }
}
