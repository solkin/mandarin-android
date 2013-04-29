package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.widget.SimpleCursorTreeAdapter;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/29/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class RosterGeneralAdapter extends SimpleCursorTreeAdapter {

    private static final String groupFrom[] = {RosterProvider.ROSTER_GROUP_NAME};
    private static final int groupTo[] = { R.id.groupName };

    private static final String childFrom[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS };
    private static final int childTo[] = { R.id.buddyId, R.id.buddyNick, R.id.buddyStatus };

    private final ContentResolver contentResolver;

    public RosterGeneralAdapter(Activity context, Cursor cursor) {
        super(context, cursor,
                R.layout.group_item, groupFrom, groupTo,
                R.layout.buddy_item, childFrom, childTo);
        this.contentResolver = context.getContentResolver();
        context.startManagingCursor(cursor);
    }

    public static Cursor getCursor(ContentResolver contentResolver) {
        return contentResolver.query(Settings.GROUP_RESOLVER_URI, null, null,
                null, RosterProvider.ROSTER_GROUP_NAME + " ASC");
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        Log.d(Settings.LOG_TAG, "getChildrenCursor");
        int columnIndex = groupCursor.getColumnIndex(RosterProvider.ROSTER_GROUP_NAME);
        String groupName = groupCursor.getString(columnIndex);
        return contentResolver.query(Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_GROUP + "='" + groupName +"'",
                null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
    }
}
