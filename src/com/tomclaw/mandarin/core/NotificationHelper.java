package com.tomclaw.mandarin.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatActivity;

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

    public void createSimpleNotification(int notificationId, String title, String message){
        if (!isNotificationForEveryContact) {
             notificationId = defaultNotificationId;
        }
        int counter;
        counter = getNotificationsCount(notificationId);
        counter += 1;
        mNotificationsCounter.put(notificationId, counter);

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

        Intent resultIntent = new Intent(mContext, ChatActivity.class);
        resultIntent.putExtra(NOTIFICATION_ID, notificationId);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent;
        //pass notificationId for creating multiple instances of Pending Intent
        resultPendingIntent = stackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        Intent dismissIntent = new Intent(NOTIFICATIONS_FILTER);
        dismissIntent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(mContext, notificationId, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
        mNotificationManager.notify(notificationId, builder.build());
    }

    public void clearNotifications(int id){
        if(mNotificationsCounter.containsKey(id)){
            mNotificationsCounter.remove(id);
        }
    }

    private int getNotificationsCount(int notificationId){
        return mNotificationsCounter.containsKey(notificationId) ? mNotificationsCounter.get(notificationId) : 0;
    }

    public void cancelAll() {
        mNotificationManager.cancelAll();
    }
}
