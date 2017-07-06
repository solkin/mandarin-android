package com.tomclaw.mandarin.im.tasks;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.MessageCursor;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.NotificationData;
import com.tomclaw.mandarin.util.NotificationLine;
import com.tomclaw.mandarin.util.Notifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by solkin on 05.07.17.
 */
public class UpdateNotificationTask extends DatabaseTask {

    private volatile long notificationCancelTime = 0;

    public UpdateNotificationTask(Context context, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(context, sqLiteDatabase, bundle);
    }

    @Override
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        String sql = "SELECT *\n" +
                "FROM chat_history\n" +
                "INNER JOIN roster_buddy ON chat_history.message_buddy_id=roster_buddy.buddy_id\n" +
                "AND chat_history.message_account_db_id=roster_buddy.account_db_id\n" +
                "WHERE buddy_id IN\n" +
                "    (SELECT buddy_id\n" +
                "     FROM roster_buddy\n" +
                "     WHERE buddy_unread_count>0)\n" +
                "  AND message_type=1\n" +
                "GROUP BY buddy_id;";
        Cursor cursor = null;
        try {
            Logger.log("notification task: sql");
            long time = System.currentTimeMillis();
            cursor = getDatabase().rawQuery(sql, null);
            Logger.log("update notifications query took " + (System.currentTimeMillis() - time) + " ms.");
            if (cursor.moveToFirst()) {
                BuddyCursor buddyCursor = new BuddyCursor(cursor);
                MessageCursor messageCursor = new MessageCursor(cursor);
                int requestCode = 1;
                int summaryUnreadCount = 0;
                boolean notify = false;
                List<NotificationLine> lines = new ArrayList<>();
                do {
                    long notifiedMessageId = buddyCursor.getNotifiedMessageId();
                    Buddy buddy = buddyCursor.toBuddy();
                    int unreadCnt = buddyCursor.getUnreadCount();
                    String title = buddyCursor.getBuddyNick();
                    MessageData messageData = messageCursor.toMessageData();
                    long messageId = messageData.getMessageId();
                    String text = messageData.getMessageText();
                    summaryUnreadCount += unreadCnt;
                    PendingIntent replyNowIntent = PendingIntent.getActivity(context, requestCode++,
                            new Intent(context, ChatActivity.class)
                                    .putExtra(Buddy.KEY_STRUCT, buddy)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    NotificationCompat.Action replyNowAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_reply, context.getString(R.string.reply_now), replyNowIntent)
                            .setAllowGeneratedReplies(true)
                            .build();
                    lines.add(new NotificationLine(title, text, null,
                            Collections.singletonList(replyNowAction)));
                    if (notifiedMessageId < messageId) {
                        notify = true;
                        QueryHelper.modifyBuddyNotifiedMessageId(databaseLayer, buddy, messageId);
                    }
                } while (cursor.moveToNext());
                String title = context.getResources().getQuantityString(R.plurals.count_new_messages, summaryUnreadCount, summaryUnreadCount);
                String text = "";
                for (NotificationLine line : lines) {
                    if (text.length() > 0) {
                        text += ", ";
                    }
                    text += line.getTitle();
                }
                PendingIntent openChatsIntent = PendingIntent.getActivity(context, requestCode,
                        new Intent(context, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_CANCEL_CURRENT);
                NotificationCompat.Action chatsAction = new NotificationCompat.Action.Builder(
                        R.drawable.ic_chat, context.getString(R.string.dialogs), openChatsIntent)
                        .setAllowGeneratedReplies(true)
                        .build();
                boolean privateNotifications = PreferenceHelper.isPrivateNotifications(context);
                NotificationData data = new NotificationData(!privateNotifications, title, text, null,
                        lines, Collections.singletonList(chatsAction));
                if (notify && isNotificationCompleted()) {
                    // Wzh-wzh!
                    onNotificationShown();
                    Logger.log("update notifications: wzh-wzh!");
//                    if (PreferenceHelper.isSound(context)) {
//                        notificationBuilder.setSound(PreferenceHelper.getNotificationUri(context));
//                    }
//                    if (PreferenceHelper.isVibrate(context)) {
//                        notificationBuilder.setVibrate(new long[]{0, 750});
//                    }
//                    if (PreferenceHelper.isLights(context)) {
//                        notificationBuilder.setLights(Settings.LED_COLOR_RGB,
//                                Settings.LED_BLINK_DELAY, Settings.LED_BLINK_DELAY);
//                    }
                }
                Notifier.showNotification(context, data);
            } else {
                onNotificationCancel();
                Notifier.hideNotification(context);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Collections.emptyList();
    }

    @Override
    protected String getOperationDescription() {
        return "update notification";
    }

    private void onNotificationCancel() {
        long notificationRemain = getNotificationRemain();
        if (notificationRemain > 0) {
            try {
                // Take some time to read this message and notification to be shown
                Thread.sleep(notificationRemain);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void onNotificationShown() {
        notificationCancelTime = System.currentTimeMillis() + Settings.NOTIFICATION_MIN_DELAY;
    }

    private boolean isNotificationCompleted() {
        return getNotificationRemain() <= 0;
    }

    private long getNotificationRemain() {
        return notificationCancelTime - System.currentTimeMillis();
    }
}
