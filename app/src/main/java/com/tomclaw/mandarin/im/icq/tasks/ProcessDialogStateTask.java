package com.tomclaw.mandarin.im.icq.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqStatusUtil;
import com.tomclaw.mandarin.im.icq.dto.HistDlgState;
import com.tomclaw.mandarin.im.icq.dto.Message;
import com.tomclaw.mandarin.im.tasks.HistoryMergeTask;

import java.util.ArrayList;
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
                final int statusIndex = StatusUtil.STATUS_OFFLINE;
                final String statusTitle = IcqStatusUtil.getStatusTitle(accountType, null, statusIndex);
                final String buddyNick = histDlgState.getPersons().get(0).getFriendly();
                final String statusMessage = "";
                final String buddyIcon = null;
                final long lastSeen = -1;
                final long updateTime = System.currentTimeMillis();

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
        ArrayList<MessageData> messages = new ArrayList<>();
        long prevMsgId = -1;
        for (Message message : histDlgState.getMessages()) {
            int messageType = message.isOutgoing() ?
                    GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING :
                    GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
            long messageTime = TimeUnit.SECONDS.toMillis(message.getTime());

            MessageData messageData = new MessageData(accountDbId, buddyId, prevMsgId,
                    message.getMsgId(), message.getReqId(), messageType, messageTime, message.getText());
            messages.add(messageData);
            if (messageTime > lastMessageTime) {
                lastMessageTime = messageTime;
            }
        }
        if (!messages.isEmpty()) {
            Bundle mergeBundle = new Bundle();
            mergeBundle.putSerializable(HistoryMergeTask.KEY_MESSAGES, messages);
            HistoryMergeTask historyMergeTask = new HistoryMergeTask(context, sqLiteDatabase, mergeBundle);
            historyMergeTask.executeBackground();
        }

        if (lastMessageTime == Long.MIN_VALUE) {
            lastMessageTime = null;
        }

        long unreadCnt = histDlgState.getUnreadCnt();
        long lastMsgId = histDlgState.getLastMsgId();
        long yoursLastRead = histDlgState.getYoursLastRead();
        long theirsLastDelivered = histDlgState.getTheirsLastDelivered();
        long theirsLastRead = histDlgState.getTheirsLastRead();
        long delUpTo = histDlgState.getDelUpTo();
        String patchVersion = histDlgState.getPatchVersion();

        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = QueryHelper.getBuddyCursor(databaseLayer, buddy);
            long localYoursLastRead = buddyCursor.getYoursLastRead();
            yoursLastRead = Math.max(yoursLastRead, localYoursLastRead);
            delUpTo = Math.max(delUpTo, buddyCursor.getDelUpTo());
            String localPatchVersion = buddyCursor.getPatchVersion();
            if (!TextUtils.equals(patchVersion, localPatchVersion)) {
                // TODO: now we must invalidate all history?!
            }
            if (delUpTo != localYoursLastRead) {
                QueryHelper.removeMessagesUpTo(databaseLayer, buddy, histDlgState.getDelUpTo());
                QueryHelper.markMessageRequested(databaseLayer, buddy, histDlgState.getDelUpTo());
            }
            if (!buddyCursor.getDialog() && histDlgState.isStarting()) {
                QueryHelper.modifyDialog(databaseLayer, buddy, true);
            }
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }

        QueryHelper.modifyDialogState(databaseLayer, buddy, unreadCnt, lastMessageTime,
                lastMsgId, yoursLastRead, theirsLastDelivered, theirsLastRead, patchVersion);
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
