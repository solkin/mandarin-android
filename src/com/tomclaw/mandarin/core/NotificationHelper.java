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
    private Map<Integer, Integer> mNotificationsCounter;

    private static final int defaultNotificationId = -1;

    public NotificationHelper(Context context){
        mContext = context;
        mResolver = mContext.getContentResolver();
        mNotificationsCounter = new HashMap<Integer, Integer>();
    }

    public void createMessageNotification(int id,  String title, String message){
        int counter;
        if(mNotificationsCounter.containsKey(id)){
            counter = mNotificationsCounter.get(id);
        } else {
            counter = 0;
        }
        counter += 1;
        mNotificationsCounter.put(id, counter);

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
        resultIntent.putExtra("id", id);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent;
        //pass id for creating multiple instances of Pending Intent
        resultPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        Intent dismissIntent = new Intent("Notifications");
        dismissIntent.putExtra("id", id);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(mContext, id, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, builder.build());
    }

    public void clearNotifications(int id){
        if(mNotificationsCounter.containsKey(id)){
            mNotificationsCounter.remove(id);
        }
    }

}
