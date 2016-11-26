package com.tomclaw.mandarin.im.icq.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqStatusUtil;
import com.tomclaw.mandarin.im.icq.dto.HistDlgState;
import com.tomclaw.mandarin.im.icq.dto.Message;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tomclaw.mandarin.core.GlobalProvider.CHAT_HISTORY_TABLE;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_MESSAGE_ID;

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

        Long lastMessageTime = Long.MIN_VALUE;

        SQLiteDatabase sqLiteDatabase = getDatabase();
        for (Message message : histDlgState.getMessages()) {
            int messageType = message.isOutgoing() ?
                    GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING :
                    GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
            long prevMsgId = -1;
            long messageTime = TimeUnit.SECONDS.toMillis(message.getTime());

            insertTextMessage(
                    sqLiteDatabase,
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
        if (lastMessageTime == Long.MIN_VALUE) {
            lastMessageTime = null;
        }

        long unreadCnt = histDlgState.getUnreadCnt();
        long lastMsgId = histDlgState.getLastMsgId();
        long yoursLastRead = histDlgState.getYoursLastRead();
        long theirsLastDelivered = histDlgState.getTheirsLastDelivered();
        long theirsLastRead = histDlgState.getTheirsLastRead();

        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = QueryHelper.getBuddyCursor(databaseLayer, buddy);
            lastMsgId = Math.max(lastMsgId, buddyCursor.getLastMessageId());
            yoursLastRead = Math.max(yoursLastRead, buddyCursor.getYoursLastRead());
            theirsLastDelivered = Math.max(theirsLastDelivered, buddyCursor.getTheirsLastDelivered());
            theirsLastRead = Math.max(theirsLastRead, buddyCursor.getTheirsLastRead());
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }

        QueryHelper.modifyDialogState(databaseLayer, buddy, unreadCnt, lastMessageTime,
                lastMsgId, yoursLastRead, theirsLastDelivered, theirsLastRead);

        if (histDlgState.getDelUpTo() != null) {
            QueryHelper.removeMessagesUpTo(databaseLayer, buddy, histDlgState.getDelUpTo());
        }
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Arrays.asList(Settings.HISTORY_RESOLVER_URI, Settings.BUDDY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "process dialog state";
    }

    public static void insertTextMessage(@NonNull SQLiteDatabase sqLiteDatabase,
                                         @NonNull Buddy buddy,
                                         long prevMsgId,
                                         long msgId,
                                         int messageType,
                                         @Nullable String cookie,
                                         long messageTime,
                                         @NonNull String messageText) {
        Logger.log("insertTextMessage: type: " + messageType + " message = " + messageText);

        int accountDbId = buddy.getAccountDbId();
        String buddyId = buddy.getBuddyId();

        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ID, buddyId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, prevMsgId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_ID, msgId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        if (cookie == null) {
            contentValues.putNull(GlobalProvider.HISTORY_MESSAGE_COOKIE);
        } else {
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        }
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);

        long rowId = sqLiteDatabase.insertWithOnConflict(CHAT_HISTORY_TABLE, null,
                contentValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (rowId == -1) {
            Logger.log("insertTextMessage: message present: " + msgId + " and will be updated");
            contentValues.clear();
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, prevMsgId);
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
            String select = new QueryBuilder()
                    .columnEquals(HISTORY_MESSAGE_ID, msgId)
                    .getSelect();
            sqLiteDatabase.update(CHAT_HISTORY_TABLE, contentValues, select, null);
        }
    }
}
