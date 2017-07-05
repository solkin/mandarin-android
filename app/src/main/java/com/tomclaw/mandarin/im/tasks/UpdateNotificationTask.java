package com.tomclaw.mandarin.im.tasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by solkin on 05.07.17.
 */
public class UpdateNotificationTask extends DatabaseTask {

    private static final int NOTIFICATION_ID = 0x01;

    private volatile long notificationCancelTime = 0;

    public UpdateNotificationTask(Context context, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(context, sqLiteDatabase, bundle);
    }

    @Override
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        Collection<Buddy> buddies = QueryHelper.getBuddiesWithUnread(databaseLayer);
        if (buddies.isEmpty()) {
            onNotificationCancel();
        } else {
            int summaryUnreadCount = 0;
            boolean notify = false;
            List<Notification> notifications = new ArrayList<>();
            for (Buddy buddy : buddies) {
                BuddyCursor buddyCursor = null;
                try {
                    buddyCursor = QueryHelper.getBuddyCursor(databaseLayer, buddy);
                    long notifiedMessageId = buddyCursor.getNotifiedMessageId();
                    MessageData messageData = QueryHelper.getLastIncomingMessage(databaseLayer, buddy);
                    long messageId = messageData.getMessageId();
                    int unreadCnt = buddyCursor.getUnreadCount();
                    String title = buddyCursor.getBuddyNick();
                    String content = messageData.getMessageText();
                    summaryUnreadCount += unreadCnt;
                    PendingIntent replyNowIntent = PendingIntent.getActivity(context, 1,
                            new Intent(context, ChatActivity.class)
                                    .putExtra(Buddy.KEY_STRUCT, buddy)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle(title)
                                    .setContentText(content)
                                    .setGroup("GROUP_1")
                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                                    .setContentIntent(replyNowIntent);
                    notifications.add(builder.build());
                    if (notifiedMessageId < messageId) {
                        notify = true;
                        QueryHelper.modifyBuddyNotifiedMessageId(databaseLayer, buddy, messageId);
                    }
                } finally {
                    if (buddyCursor != null) {
                        buddyCursor.close();
                    }
                }
            }
            if (notify) {
                // Wzh-wzh!
                Logger.log("Wzh-wzh!");
            }
            String title = context.getResources().getQuantityString(R.plurals.count_new_messages, summaryUnreadCount, summaryUnreadCount);
            String content = "";
            PendingIntent openChatsIntent = PendingIntent.getActivity(context, 3,
                    new Intent(context, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder groupBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setGroupSummary(true)
                            .setGroup("GROUP_1")
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                            .setContentIntent(openChatsIntent);
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(NOTIFICATION_ID, groupBuilder.build());
            int c = NOTIFICATION_ID;
            for (Notification notification : notifications) {
                manager.notify(++c, notification);
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
