package com.tomclaw.mandarin.main.tasks;

import android.content.ContentResolver;
import android.content.Context;

import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PleaseWaitTask;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;

import java.util.Collection;

/**
 * Created by Solkin on 13.06.2014.
 */
public class BuddyRemoveTask extends PleaseWaitTask {

    private Collection<Integer> buddyDbIds;

    public BuddyRemoveTask(Context context, Collection<Integer> buddyDbIds) {
        super(context);
        this.buddyDbIds = buddyDbIds;
    }

    @Override
    public void executeBackground() throws Throwable {
        Context context = getWeakObject();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            for (int buddyDbId : buddyDbIds) {
                BuddyCursor buddyCursor = null;
                try {
                    buddyCursor = QueryHelper.getBuddyCursor(contentResolver, buddyDbId);
                    int accountDbId = buddyCursor.getBuddyAccountDbId();
                    String groupName = buddyCursor.getBuddyGroup();
                    String buddyId = buddyCursor.getBuddyId();
                    // Mark as removing.
                    QueryHelper.modifyOperation(contentResolver, buddyDbId,
                            GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE);
                    // Remove request.
                    RequestHelper.requestRemove(contentResolver, accountDbId, groupName, buddyId);
                } catch (BuddyNotFoundException ignored) {
                    // Wha-a-a-at?! No buddy found.
                } finally {
                    if (buddyCursor != null) {
                        buddyCursor.close();
                    }
                }
            }
        }
    }
}
