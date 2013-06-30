package com.tomclaw.mandarin.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private Context mContext;
    private ContentResolver mResolver;
    private NotificationManager mNotificationManager;
    private Map<Integer, Integer> mNotificationsCounter;

    // Notification id for common app events (not for messages from contacts)
    private static final int defaultNotificationId = -1;

    // take from preferences
    private boolean isNotificationForEveryContact = true;

    public static final int NOT_EXISTS_NOTIFICATION_ID = -2;
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String NOTIFICATIONS_FILTER = "notifications";

    public NotificationHelper(Context context){
        mContext = context;
        mResolver = mContext.getContentResolver();
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationsCounter = new HashMap<Integer, Integer>();
    }

    public void clearNotifications(int id){
        if(mNotificationsCounter.containsKey(id)){
            mNotificationsCounter.remove(id);
        }
    }

    public void cancelAll() {
        mNotificationManager.cancelAll();
    }

    public void cancel(int notificationId){
        mNotificationManager.cancel(notificationId);
    }

    public void notifyAboutSimpleEvent(int notificationId, String title, String message){
        if (!isNotificationForEveryContact) {
            notificationId = defaultNotificationId;
        }
        int counter = prepareNotificationsCounter(notificationId);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_launcher))
                        .setSmallIcon(R.drawable.ic_inc_bubble)
                        .setAutoCancel(true);
        if(counter > 1){
            builder.setNumber(counter);
        }

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.putExtra(NOTIFICATION_ID, notificationId);
        builder.setContentIntent(getResultPendingIntent(notificationId, resultIntent));

        Intent dismissIntent = new Intent(NOTIFICATIONS_FILTER);
        dismissIntent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(mContext, notificationId, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
        mNotificationManager.notify(notificationId, builder.build());
    }

    public void notifyAboutMessage(int buddyDbId, String message){
        int notificationId = buddyDbId;
        if (!isNotificationForEveryContact) {
            notificationId = defaultNotificationId;
        }
        int counter = prepareNotificationsCounter(notificationId);

        Cursor cursor = mResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);

        String nick;
        Bitmap icon;
        if (cursor.moveToFirst()) {
            int buddyNickIndex = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            nick = cursor.getString(buddyNickIndex);
        } else {
            return;
        }
        icon = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_launcher);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(nick)
                        .setContentText(message)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_inc_bubble)
                        .setAutoCancel(true);
        if(counter > 1){
            builder.setNumber(counter);
        }

        Intent resultIntent = new Intent(mContext, ChatActivity.class);
        resultIntent.putExtra(NOTIFICATION_ID, notificationId);
        resultIntent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        builder.setContentIntent(getResultPendingIntent(notificationId, resultIntent));

        Intent dismissIntent = new Intent(NOTIFICATIONS_FILTER);
        dismissIntent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(mContext, notificationId, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);

        mNotificationManager.notify(notificationId, builder.build());
    }

    public void notifyAboutMessage(int accountDbId, String buddyId, String message){
        Cursor helpCursor = mResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID + "='" + accountDbId + "'" + " AND "
                        + GlobalProvider.ROSTER_BUDDY_ID + "='" + buddyId + "'", null, null);
        int buddyDbId;
        if (helpCursor.moveToFirst()) {
            final int BUDDY_DB_ID_COLUMN = helpCursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            buddyDbId = helpCursor.getInt(BUDDY_DB_ID_COLUMN);
        } else {
            return;
        }

        int notificationId = buddyDbId;
        if (!isNotificationForEveryContact) {
            notificationId = defaultNotificationId;
        }
        int counter = prepareNotificationsCounter(notificationId);

        Cursor cursor = mResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);

        String nick;
        Bitmap icon;
        if (cursor.moveToFirst()) {
            int buddyNickIndex = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            nick = cursor.getString(buddyNickIndex);
        } else {
            return;
        }
        icon = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_launcher);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(nick)
                        .setContentText(message)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_inc_bubble)
                        .setAutoCancel(true);
        if(counter > 1){
            builder.setNumber(counter);
        }

        Intent resultIntent = new Intent(mContext, ChatActivity.class);
        resultIntent.putExtra(NOTIFICATION_ID, notificationId);
        resultIntent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        builder.setContentIntent(getResultPendingIntent(notificationId, resultIntent));

        Intent dismissIntent = new Intent(NOTIFICATIONS_FILTER);
        dismissIntent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(mContext, notificationId, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);

        mNotificationManager.notify(notificationId, builder.build());
    }

    public void notifyAboutAuthorizationMessage(int buddyDbId, String message){
        int notificationId = defaultNotificationId;
        if (!isNotificationForEveryContact) {
            notificationId = defaultNotificationId;
        }
        int counter = prepareNotificationsCounter(notificationId);

        Cursor cursor = mResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);

        String nick;
        Bitmap icon;
        if (cursor.moveToFirst()) {
            int buddyNickIndex = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            nick = cursor.getString(buddyNickIndex);
        } else {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(nick)
                        .setContentText(message)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_launcher))
                        .setSmallIcon(R.drawable.ic_inc_bubble)
                        .setAutoCancel(true);
        if(counter > 1){
            builder.setNumber(counter);
        }

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.putExtra(NOTIFICATION_ID, notificationId);
        resultIntent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        builder.setContentIntent(getResultPendingIntent(notificationId, resultIntent));

        Intent dismissIntent = new Intent(NOTIFICATIONS_FILTER);
        dismissIntent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(mContext, notificationId, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);

        mNotificationManager.notify(notificationId, builder.build());
    }

    private int prepareNotificationsCounter(int notificationId){
        int counter;
        counter = getNotificationsCount(notificationId);
        counter += 1;
        mNotificationsCounter.put(notificationId, counter);
        return counter;
    }

    private int getNotificationsCount(int notificationId){
        return mNotificationsCounter.containsKey(notificationId) ? mNotificationsCounter.get(notificationId) : 0;
    }

    private PendingIntent getResultPendingIntent(int pendingIntentId, Intent resultIntent){
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(pendingIntentId, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
