package com.tomclaw.mandarin.im.icq.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqStatusUtil;
import com.tomclaw.mandarin.im.icq.dto.HistDlgState;
import com.tomclaw.mandarin.im.icq.dto.Message;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ivsolkin on 23.11.16.
 * Task for processing history dialog state - update buddy flags and insert messages
 */
public class ProcessDialogStateTask extends DatabaseTask {

    public static String KEY_ACCOUNT_DB_ID = "key_account_db_id";
    public static String KEY_ACCOUNT_TYPE = "key_account_type";
    public static String KEY_IGNORE_UNKNOWN = "key_ignore_unknown";
    public static String KEY_RECYCLE_STRING = "key_recycle_string";
    public static String KEY_DIALOG_STATE = "key_dialog_state";

    public ProcessDialogStateTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object, sqLiteDatabase, bundle);
    }

    @Override
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        int accountDbId = bundle.getInt(KEY_ACCOUNT_DB_ID);
        String accountType = bundle.getString(KEY_ACCOUNT_TYPE);
        boolean isIgnoreUnknown = bundle.getBoolean(KEY_IGNORE_UNKNOWN);
        String recycleString = bundle.getString(KEY_RECYCLE_STRING);
        HistDlgState histDlgState = (HistDlgState) bundle.getSerializable(KEY_DIALOG_STATE);
        if (histDlgState == null) {
            return;
        }
        String buddyId = histDlgState.getSn();
        Buddy buddy = new Buddy(accountDbId, buddyId);

        boolean buddyExist = QueryHelper.checkBuddy(databaseLayer, accountDbId, buddyId);
        if (!buddyExist) {
            if (isIgnoreUnknown) {
                return;
            } else {
                int statusIndex = StatusUtil.STATUS_OFFLINE;
                String statusTitle = IcqStatusUtil.getStatusTitle(accountType, null, statusIndex);
                String buddyNick = histDlgState.getPersons().get(0).getFriendly();
                String statusMessage = "";
                String buddyIcon = null;
                long lastSeen = -1;
                long updateTime = System.currentTimeMillis();

                QueryHelper.updateOrCreateBuddy(
                        databaseLayer,
                        accountDbId,
                        accountType,
                        updateTime,
                        GlobalProvider.GROUP_ID_RECYCLE,
                        recycleString,
                        buddyId,
                        buddyNick,
                        statusIndex,
                        statusTitle,
                        statusMessage,
                        buddyIcon,
                        lastSeen);
            }
        }

        long lastMessageTime = Long.MIN_VALUE;
        for (Message message : histDlgState.getMessages()) {
            int messageType = message.isOutgoing() ?
                    GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING :
                    GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
            long prevMsgId = -1;
            long messageTime = TimeUnit.SECONDS.toMillis(message.getTime());
            QueryHelper.insertTextMessage(
                    databaseLayer,
                    buddy,
                    prevMsgId,
                    message.getMsgId(),
                    messageType,
                    message.getReqId(),
                    messageTime,
                    message.getText());
            if (messageTime > lastMessageTime) {
                lastMessageTime = messageTime;
            }
        }

        // TODO: calculate values with remote and local data
        // TODO: keep in mind, that dialog state may come without messages at all
        long unreadCnt = histDlgState.getUnreadCnt();
        long lastMsgId = histDlgState.getLastMsgId();
        long yoursLastRead = histDlgState.getYours().getLastRead();
        long theirsLastDelivered = histDlgState.getTheirs().getLastDelivered();
        long theirsLastRead = histDlgState.getTheirs().getLastRead();

        QueryHelper.modifyDialogState(databaseLayer, buddy, unreadCnt, lastMessageTime,
                lastMsgId, yoursLastRead, theirsLastDelivered, theirsLastRead);

        QueryHelper.removeMessagesUpTo(databaseLayer, buddy, histDlgState.getDelUpTo());
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Arrays.asList(Settings.HISTORY_RESOLVER_URI, Settings.BUDDY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "process dialog state";
    }
}
