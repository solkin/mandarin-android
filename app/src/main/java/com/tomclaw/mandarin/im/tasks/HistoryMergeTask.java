package com.tomclaw.mandarin.im.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.helpers.Timer;

import java.util.Collections;
import java.util.List;

import static com.tomclaw.mandarin.core.QueryHelper.insertOrUpdateMessage;

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
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) {
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

    @Override
    protected List<Uri> getModifiedUris() {
        return Collections.singletonList(Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "merge history";
    }
}
