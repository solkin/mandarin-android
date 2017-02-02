package com.tomclaw.mandarin.im.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.Timer;

import java.util.Collections;
import java.util.List;

import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_BUDDY_ID;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_MESSAGE_ID;

/**
 * Created by ivsolkin on 26.11.16.
 */

public class HistoryMergeTask extends DatabaseTask {

    public static String KEY_MESSAGES = "key_messages";
    public static String KEY_LAST_MESSAGE_BUDDY = "key_last_message_buddy";
    public static String KEY_LAST_MESSAGE_PREV_ID = "key_last_message_prev_id";
    public static String KEY_LAST_MESSAGE_ID = "key_last_message_id";

    public HistoryMergeTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object, sqLiteDatabase, bundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        Timer timer = new Timer().start();
        List<MessageData> messages = (List<MessageData>) bundle.getSerializable(KEY_MESSAGES);
        if (messages == null) {
            return;
        }
        for (MessageData message : messages) {
            insertOrUpdateMessage(databaseLayer, message);
        }

        Buddy buddy = bundle.getParcelable(KEY_LAST_MESSAGE_BUDDY);
        long lastMessagePrevId = bundle.getLong(KEY_LAST_MESSAGE_PREV_ID);
        long lastMessageId = bundle.getLong(KEY_LAST_MESSAGE_ID);
        if (buddy != null && lastMessageId != 0) {
            QueryHelper.modifyMessagePrevId(databaseLayer, buddy, lastMessageId, lastMessagePrevId);
        }
        Logger.log("messages processing time: " + timer.stop()
                + " (" + messages.size() + " messages)");
    }

    private QueryBuilder messageQueryBuilder(MessageData message) {
        return new QueryBuilder()
                .columnEquals(HISTORY_BUDDY_ACCOUNT_DB_ID, message.getBuddyAccountDbId()).and()
                .columnEquals(HISTORY_BUDDY_ID, message.getBuddyId()).and()
                .columnEquals(HISTORY_MESSAGE_ID, message.getMessageId());
    }

    private void insertOrUpdateMessage(DatabaseLayer databaseLayer, MessageData message) {
        boolean isUpdate = isMessageExist(databaseLayer, message);
        Logger.log("insertTextMessage: " + message.getMessageId() + " will be "
                + (isUpdate ? "updated" : "inserted"));
        if (isUpdate) {
            updateMessage(databaseLayer, message);
        } else {
            insertMessage(databaseLayer, message);
        }
    }

    private boolean isMessageExist(DatabaseLayer databaseLayer, MessageData message) {
        Cursor cursor = null;
        try {
            cursor = messageQueryBuilder(message)
                    .query(databaseLayer, Settings.HISTORY_RESOLVER_URI);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // TODO: add content support
    private void insertMessage(DatabaseLayer databaseLayer, MessageData message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, message.getBuddyAccountDbId());
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ID, message.getBuddyId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, message.getMessagePrevId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_ID, message.getMessageId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, message.getMessageType());
        if (message.getCookie() == null) {
            contentValues.putNull(GlobalProvider.HISTORY_MESSAGE_COOKIE);
        } else {
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, message.getCookie());
        }
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, message.getMessageTime());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, message.getMessageText());
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);
        databaseLayer.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    // TODO: add content support
    private void updateMessage(DatabaseLayer databaseLayer, MessageData message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, message.getMessagePrevId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, message.getMessageTime());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, message.getMessageText());
        messageQueryBuilder(message)
                .update(databaseLayer, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Collections.singletonList(Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "merge history";
    }
}
