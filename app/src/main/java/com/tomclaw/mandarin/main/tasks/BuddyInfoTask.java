package com.tomclaw.mandarin.main.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.BuddyInfoActivity;

public class BuddyInfoTask extends WeakObjectTask<Context> {

    private final Buddy buddy;

    public BuddyInfoTask(Context context, Buddy buddy) {
        super(context);
        this.buddy = buddy;
    }

    @Override
    public void executeBackground() throws Throwable {
        // Get context from weak reference.
        Context context = getWeakObject();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            // Obtain basic buddy info.
            BuddyCursor cursor = QueryHelper.getBuddyCursor(databaseLayer, accountDbId, buddyId);
            if (cursor != null) {
                // Cursor may have more than only one entry.
                if (cursor.moveToFirst()) {
                    String accountType = cursor.getBuddyAccountType();
                    String buddyNick = cursor.getBuddyNick();
                    String avatarHash = cursor.getBuddyAvatarHash();
                    int buddyStatus = cursor.getBuddyStatus();
                    String buddyStatusTitle = cursor.getBuddyStatusTitle();
                    String buddyStatusMessage = cursor.getBuddyStatusMessage();
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
