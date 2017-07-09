package com.tomclaw.mandarin.im.tasks;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.GlobalProvider;
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
import com.tomclaw.mandarin.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by solkin on 05.07.17.
 */
public class UpdateNotificationTask extends DatabaseTask {

    private static final String QUERY_TEMPLATE = "SELECT * " +
            "FROM {chat_history} " +
            "INNER JOIN {roster_buddy} ON {chat_history}.{message_buddy_id}={roster_buddy}.{buddy_id} " +
            "AND {chat_history}.{message_account_db_id}={roster_buddy}.{account_db_id} " +
            "WHERE {message_buddy_id} IN " +
            "    (SELECT {buddy_id} " +
            "     FROM {roster_buddy} " +
            "     WHERE {buddy_unread_count}>0) " +
            "  AND {message_type}={type_incoming} " +
            "GROUP BY {message_buddy_id};";

    private volatile long notificationCancelTime = 0;
    private final String query;

    public UpdateNotificationTask(Context context, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(context, sqLiteDatabase, bundle);
        Map<String, String> patterns = new HashMap<>();
        patterns.put("chat_history", GlobalProvider.CHAT_HISTORY_TABLE);
        patterns.put("roster_buddy", GlobalProvider.ROSTER_BUDDY_TABLE);
        patterns.put("message_buddy_id", GlobalProvider.HISTORY_BUDDY_ID);
        patterns.put("buddy_id", GlobalProvider.ROSTER_BUDDY_ID);
        patterns.put("message_account_db_id", GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
        patterns.put("account_db_id", GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID);
        patterns.put("buddy_unread_count", GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT);
        patterns.put("message_type", GlobalProvider.HISTORY_MESSAGE_TYPE);
        patterns.put("type_incoming", String.valueOf(GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING));
        query = StringUtil.format(QUERY_TEMPLATE, patterns);
    }

    @Override
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        Cursor cursor = null;
        try {
            Logger.log("notification task: start");
            long time = System.currentTimeMillis();
            cursor = getDatabase().rawQuery(query, null);
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
                    PendingIntent openChatIntent = createOpenChatIntent(context, buddy, requestCode++);
                    PendingIntent replyIntent = createReplyIntent(context, buddy, requestCode++);
                    PendingIntent readIntent = createReadIntent(context, buddy, requestCode++);
                    RemoteInput remoteInput = new RemoteInput.Builder(CoreService.KEY_REPLY_ON_MESSAGE)
                            .setLabel(context.getString(R.string.enter_your_message))
                            .build();
                    NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_reply, context.getString(R.string.reply_now), replyIntent)
                            .addRemoteInput(remoteInput)
                            .setAllowGeneratedReplies(true)
                            .build();
                    NotificationCompat.Action readAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_action_read, context.getString(R.string.mark_as_read), readIntent)
                            .setAllowGeneratedReplies(true)
                            .build();
                    lines.add(new NotificationLine(title, text, null, openChatIntent,
                            Arrays.asList(replyAction, readAction)));
                    if (notifiedMessageId < messageId) {
                        notify = true;
                        QueryHelper.modifyBuddyNotifiedMessageId(databaseLayer, buddy, messageId);
                    }
                } while (cursor.moveToNext());

                boolean privateNotifications = PreferenceHelper.isPrivateNotifications(context);

                PendingIntent contentAction;
                List<NotificationCompat.Action> actions;

                String title;
                String text;
                Bitmap image;
                if (lines.size() > 1 || privateNotifications) {
                    PendingIntent openChatsIntent = createOpenChatsIntent(context, requestCode++);
                    PendingIntent readAllIntent = createReadAllIntent(context, requestCode);
                    NotificationCompat.Action chatsAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_chat, context.getString(R.string.dialogs), openChatsIntent)
                            .setAllowGeneratedReplies(true)
                            .build();
                    NotificationCompat.Action readAllAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_action_read, context.getString(R.string.mark_as_read_all), readAllIntent)
                            .setAllowGeneratedReplies(true)
                            .build();

                    title = context.getResources().getQuantityString(R.plurals.count_new_messages, summaryUnreadCount, summaryUnreadCount);
                    text = "";
                    for (NotificationLine line : lines) {
                        if (text.length() > 0) {
                            text += ", ";
                        }
                        text += line.getTitle();
                    }
                    image = null;
                    contentAction = openChatsIntent;
                    actions = Arrays.asList(chatsAction, readAllAction);
                } else {
                    NotificationLine line = lines.remove(0);
                    title = line.getTitle();
                    text = line.getText();
                    image = line.getImage();
                    contentAction = line.getContentAction();
                    actions = line.getActions();
                }
                boolean isAlert = false;
                if (notify && isNotificationCompleted()) {
                    // Wzh-wzh!
                    onNotificationShown();
                    isAlert = true;
                    Logger.log("update notifications: wzh-wzh!");
                }
                NotificationData data = new NotificationData(!privateNotifications, isAlert,
                        title, text, image, lines, contentAction, actions);
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

    private static PendingIntent createOpenChatsIntent(Context context, int requestCode) {
        return PendingIntent.getActivity(context, requestCode,
                new Intent(context, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReadAllIntent(Context context, int requestCode) {
        return PendingIntent.getService(context, requestCode,
                new Intent(context, CoreService.class)
                        .putExtra(CoreService.EXTRA_READ_MESSAGES, true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReadIntent(Context context, Buddy buddy, int requestCode) {
        return PendingIntent.getService(context, requestCode,
                new Intent(context, CoreService.class)
                        .putExtra(CoreService.EXTRA_READ_MESSAGES, true)
                        .putExtra(Buddy.KEY_STRUCT, buddy)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createOpenChatIntent(Context context, Buddy buddy, int requestCode) {
        return PendingIntent.getActivity(context, requestCode,
                new Intent(context, ChatActivity.class)
                        .putExtra(Buddy.KEY_STRUCT, buddy)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReplyIntent(Context context, Buddy buddy, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return PendingIntent.getService(context, requestCode,
                    new Intent(context, CoreService.class)
                            .putExtra(CoreService.EXTRA_REPLY_ON_MESSAGE, true)
                            .putExtra(Buddy.KEY_STRUCT, buddy)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            return PendingIntent.getActivity(context, requestCode,
                    new Intent(context, ChatActivity.class)
                            .putExtra(Buddy.KEY_STRUCT, buddy)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }
}
