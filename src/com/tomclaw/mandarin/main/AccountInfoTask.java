package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.Task;

import java.lang.ref.WeakReference;

/**
 * Created by solkin on 12/26/13.
 */
public class AccountInfoTask extends Task {

    private WeakReference<Context> weakContext;
    private final int accountDbId;

    public AccountInfoTask(Context context, int accountDbId) {
        weakContext = new WeakReference<Context>(context);
        this.accountDbId = accountDbId;
    }

    @Override
    public void executeBackground() throws Throwable {
        // Get context from weak reference.
        Context context = weakContext.get();
        if (context != null) {
            // Obtain basic account info.
            Cursor cursor = context.getContentResolver().query(Settings.ACCOUNT_RESOLVER_URI, null,
                    GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null, null);
            if (cursor != null) {
                // Cursor may not have more than only one entry.
                if (cursor.moveToFirst()) {
                    String accountType = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE));
                    String accountId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID));
                    String accountNick = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME));
                    String avatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_AVATAR_HASH));
                    int accountStatus = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS));
                    // String buddyStatusTitle = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE));
                    // String buddyStatusMessage = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE));
                    // Now we ready to start buddy info activity.
                    context.startActivity(new Intent(context, AccountInfoActivity.class)
                            .putExtra(AccountInfoActivity.ACCOUNT_DB_ID, accountDbId)
                            .putExtra(AccountInfoActivity.BUDDY_ID, accountId)
                            .putExtra(AccountInfoActivity.BUDDY_NICK, accountNick)
                            .putExtra(AccountInfoActivity.BUDDY_AVATAR_HASH, avatarHash)

                            .putExtra(AccountInfoActivity.ACCOUNT_TYPE, accountType)
                            .putExtra(AccountInfoActivity.BUDDY_STATUS, accountStatus)
                            // .putExtra(BuddyInfoActivity.BUDDY_STATUS_TITLE, buddyStatusTitle)
                            // .putExtra(BuddyInfoActivity.BUDDY_STATUS_MESSAGE, buddyStatusMessage)
                    );
                }
                cursor.close();
            }
        }
    }

    @Override
    public void onFailMain() {
        // Get context from weak reference.
        Context context = weakContext.get();
        if (context != null) {
            Toast.makeText(context, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
        }
    }
}
