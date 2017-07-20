package com.tomclaw.mandarin.im.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by solkin on 05.07.17.
 */
public class UpdateLastReadTask extends DatabaseTask {

    public static String KEY_BUDDY = "key_buddy";

    public UpdateLastReadTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object, sqLiteDatabase, bundle);
    }

    @Override
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        Collection<Buddy> buddies;
        Buddy bundleBuddy = bundle.getParcelable(KEY_BUDDY);
        if (bundleBuddy != null) {
            buddies = Collections.singleton(bundleBuddy);
        } else {
            buddies = QueryHelper.getBuddiesWithUnread(databaseLayer);
        }
        for (Buddy buddy : buddies) {
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            long lastIncoming = QueryHelper.getLastIncomingMessageId(databaseLayer, buddy);
            long yoursLastRead = QueryHelper.getBuddyYoursLastRead(databaseLayer, buddy);
            if (lastIncoming > yoursLastRead) {
                QueryHelper.modifyBuddyYoursReadState(databaseLayer, buddy, 0, lastIncoming);
                RequestHelper.requestSetDialogState(databaseLayer, accountDbId, buddyId, lastIncoming);
            }
        }
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Arrays.asList(Settings.BUDDY_RESOLVER_URI, Settings.REQUEST_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "update last read";
    }
}
