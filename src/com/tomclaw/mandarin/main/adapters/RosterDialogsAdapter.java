package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/29/13
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class RosterDialogsAdapter extends SimpleCursorAdapter {

    private static final String from[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS };
    private static final int to[] = { R.id.buddyId, R.id.buddyNick, R.id.buddyStatus };

    public RosterDialogsAdapter(Activity context, Cursor cursor) {
        super(context, R.layout.buddy_item, cursor, from, to, 0x00);

        context.startManagingCursor(cursor);
    }

    public static Cursor getCursor(ContentResolver contentResolver) {
        return contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                RosterProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
    }
}
