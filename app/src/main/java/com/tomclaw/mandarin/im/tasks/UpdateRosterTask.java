package com.tomclaw.mandarin.im.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.im.GroupData;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.BuddyData;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ivsolkin on 22.11.16.
 * Task for merging locally saved buddy list and server's roster
 */
public class UpdateRosterTask extends DatabaseTask {

    public static String KEY_ACCOUNT_DB_ID = "key_account_db_id";
    public static String KEY_ACCOUNT_TYPE = "key_account_type";
    public static String KEY_GROUP_DATAS = "key_group_datas";

    public UpdateRosterTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object, sqLiteDatabase, bundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void runInTransaction(Context context,
                                    DatabaseLayer databaseLayer,
                                    Bundle bundle) throws Throwable {
        long updateTime = System.currentTimeMillis();
        int accountDbId = bundle.getInt(KEY_ACCOUNT_DB_ID);
        String accountType = bundle.getString(KEY_ACCOUNT_TYPE);
        List<GroupData> groupDatas = (List<GroupData>) bundle.getSerializable(KEY_GROUP_DATAS);
        if (groupDatas == null) {
            return;
        }
        for (GroupData groupData : groupDatas) {
            QueryHelper.updateOrCreateGroup(databaseLayer, accountDbId, updateTime,
                    groupData.getGroupName(), groupData.getGroupId());
            for (BuddyData buddyData : groupData.getBuddyDatas()) {
                QueryHelper.updateOrCreateBuddy(databaseLayer, accountDbId, accountType, updateTime,
                        buddyData.getGroupId(), buddyData.getGroupName(), buddyData.getBuddyId(),
                        buddyData.getBuddyNick(), buddyData.getStatusIndex(), buddyData.getStatusTitle(),
                        buddyData.getStatusMessage(), buddyData.getBuddyIcon(), buddyData.getLastSeen());
            }
        }
        QueryHelper.removeOutdatedBuddies(databaseLayer, accountDbId, updateTime);
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Arrays.asList(Settings.GROUP_RESOLVER_URI, Settings.BUDDY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "update roster";
    }
}
