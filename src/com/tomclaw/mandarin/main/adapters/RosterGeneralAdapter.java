package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.SimpleCursorTreeAdapter;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RosterProvider;
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

    private static final String groupFrom[] = {RosterProvider.ROSTER_GROUP_NAME};
    private static final int groupTo[] = {R.id.groupName};

    private static final String childFrom[] = {RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK,
            RosterProvider.ROSTER_BUDDY_STATUS};
    private static final int childTo[] = {R.id.buddyId, R.id.buddyNick, R.id.buddyStatus};

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
                    null, RosterProvider.ROSTER_GROUP_NAME + " ASC");
        } else {
            return new CursorLoader(context, Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_GROUP
                    + "='" + bundle.getString(RosterProvider.ROSTER_BUDDY_GROUP) + "'", null,
                    RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == ADAPTER_GENERAL_ID) {
            setGroupCursor(cursor);
        } else {
            setChildrenCursor(cursorLoader.getId(), cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == ADAPTER_GENERAL_ID) {
            setGroupCursor(null);
        } else {
            try {
                setChildrenCursor(cursorLoader.getId(), null);
            } catch (NullPointerException ex) {
                // Nothing to do in this case.
            }
        }
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // This will calls when collapsed group expands.
        int groupPosition = groupCursor.getPosition();
        int columnIndex = groupCursor.getColumnIndex(RosterProvider.ROSTER_GROUP_NAME);
        String groupName = groupCursor.getString(columnIndex);
        Log.d(Settings.LOG_TAG, "Child cursor for " + groupName + "(" + groupPosition + ") loading started");
        // Store group name into bundle to have opportunity build query.
        Bundle bundle = new Bundle();
        bundle.putString(RosterProvider.ROSTER_BUDDY_GROUP, groupName);
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
}
