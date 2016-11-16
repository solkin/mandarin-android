package com.tomclaw.mandarin.main.tasks;

import android.content.ContentResolver;
import android.content.Context;

import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PleaseWaitTask;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.StrictBuddy;

import java.util.Collection;

/**
 * Created by Solkin on 13.06.2014.
 */
public class BuddyRemoveTask extends PleaseWaitTask {

    private Collection<StrictBuddy> buddies;

    public BuddyRemoveTask(Context context, Collection<StrictBuddy> buddies) {
        super(context);
        this.buddies = buddies;
    }

    @Override
    public void executeBackground() throws Throwable {
        Context context = getWeakObject();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
            for (StrictBuddy buddy : buddies) {
                // Mark as removing.
                QueryHelper.modifyOperation(databaseLayer, buddy,
                        GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE);
                // Remove request.
                int accountDbId = buddy.getAccountDbId();
                String groupName = buddy.getGroupName();
                String buddyId = buddy.getBuddyId();
                RequestHelper.requestRemove(contentResolver, accountDbId, groupName, buddyId);
            }
        }
    }
}
