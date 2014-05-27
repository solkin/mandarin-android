package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;

public class BuddyInfoTask extends WeakObjectTask<Context> {

    private final int buddyDbId;

    public BuddyInfoTask(Context context, int buddyDbId) {
        super(context);
        this.buddyDbId = buddyDbId;
    }

    @Override
    public void executeBackground() throws Throwable {
        // Get context from weak reference.
        Context context = getWeakObject();
        if (context != null) {
            // Obtain basic buddy info.
            Cursor cursor = context.getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null,
                    GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
            if (cursor != null) {
                // Cursor may have more than only one entry.
                if (cursor.moveToFirst()) {
                    int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                    String accountType = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE));
                    String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                    String buddyNick = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK));
                    String avatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH));
                    int buddyStatus = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS));
                    String buddyStatusTitle = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE));
                    String buddyStatusMessage = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE));
                    // Now we ready to start buddy info activity.
                    context.startActivity(new Intent(context, BuddyInfoActivity.class)
                                    .putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, accountDbId)
                                    .putExtra(BuddyInfoRequest.BUDDY_ID, buddyId)
                                    .putExtra(BuddyInfoRequest.BUDDY_NICK, buddyNick)
                                    .putExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH, avatarHash)

                                    .putExtra(BuddyInfoRequest.ACCOUNT_TYPE, accountType)
                                    .putExtra(BuddyInfoRequest.BUDDY_STATUS, buddyStatus)
                                    .putExtra(BuddyInfoRequest.BUDDY_STATUS_TITLE, buddyStatusTitle)
                                    .putExtra(BuddyInfoRequest.BUDDY_STATUS_MESSAGE, buddyStatusMessage)
                    );
                }
                cursor.close();
            }
        }
    }

    @Override
    public void onFailMain() {
        // Get context from weak reference.
        Context context = getWeakObject();
        if (context != null) {
            Toast.makeText(context, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
        }
    }
}
