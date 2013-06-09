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
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:22 PM
 */
public class RosterGeneralAdapter extends CursorTreeAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static int COLUMN_GROUP_NAME;
    private static int COLUMN_BUDDY_NICK;
    private static int COLUMN_BUDDY_ID;
    private static int COLUMN_BUDDY_STATUS;

    private static final int ADAPTER_GENERAL_ID = -1;

    private Context context;
    private LoaderManager loaderManager;
    private LayoutInflater mInflater;

    public RosterGeneralAdapter(Context context, LoaderManager loaderManager) {
        super(null, context);
        this.context = context;
        this.loaderManager = loaderManager;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        // Checking for Id.
        if (cursorLoader.getId() == ADAPTER_GENERAL_ID) {
            // Detecting columns.
            COLUMN_GROUP_NAME = cursor.getColumnIndex(GlobalProvider.ROSTER_GROUP_NAME);
            // Trying to set cursor.
            setGroupCursor(cursor);
        } else {
            // Detecting columns.
            COLUMN_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            COLUMN_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
            COLUMN_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
            // Trying to set cursor.
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
    public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate(R.layout.group_item,
                parent, false);
    }

    @Override
    public View newChildView(Context context, Cursor cursor, boolean isLastChild,
                             ViewGroup parent) {
        return mInflater.inflate(R.layout.buddy_item, parent, false);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        String groupName = cursor.getString(COLUMN_GROUP_NAME);
        TextView groupNameView = (TextView)view.findViewById(R.id.group_name);
        groupNameView.setText(groupName);
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        // Obtain data from cursor.
        String buddyNick = cursor.getString(COLUMN_BUDDY_NICK);
        String buddyId = cursor.getString(COLUMN_BUDDY_ID);
        int buddyStatus = cursor.getInt(COLUMN_BUDDY_STATUS);
        // Find views
        TextView buddyNickView = (TextView)view.findViewById(R.id.buddy_nick);
        TextView buddyIdView = (TextView)view.findViewById(R.id.buddy_id);
        ImageView buddyStatusView = (ImageView)view.findViewById(R.id.buddy_status);
        // Update data.
        buddyNickView.setText(buddyNick);
        buddyIdView.setText(buddyId);
        buddyStatusView.setImageResource(buddyStatus);
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
            view = mInflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in roster general adapter: " + ex.getMessage());
        }
        return view;
    }

    public int getBuddyDbId(int groupPosition, int childPosition) {
        Cursor cursor = getChild(groupPosition, childPosition);
        return cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
    }
}
