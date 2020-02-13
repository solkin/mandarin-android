package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

import static android.app.NotificationManager.IMPORTANCE_LOW;

/**
 * Created by Solkin on 21.10.2014.
 */
public abstract class NotifiableUploadRequest<A extends AccountRoot> extends RangedUploadRequest<A> {

    private static final int NOTIFICATION_ID = 0x02;
    private static final String NOTIFICATION_CHANNEL_ID = "file_sharing";

    private transient NotificationCompat.Builder mBuilder;
    private transient NotificationManager mNotifyManager;

    private transient long progressUpdateTime = 0;

    @Override
    protected final void onStarted() throws Throwable {
        Context context = getAccountRoot().getContext();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = context.getString(R.string.file_sharing);
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    channelName,
                    IMPORTANCE_LOW
            );
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
        mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        mBuilder.setContentTitle(context.getString(R.string.file_upload_title))
                .setContentText(getDescription())
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .setProgress(0, 100, true)
                .setContentIntent(getIntent());
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        // Delegate invocation.
        onStartedDelegate();
    }

    protected abstract PendingIntent getIntent();

    protected abstract String getDescription();

    protected abstract void onStartedDelegate() throws Throwable;

    @Override
    protected final void onBufferReleased(long sent, long size) {
        final int progress = (int) (100 * sent / size);
        if (System.currentTimeMillis() - progressUpdateTime >= getProgressStepDelay()) {
            mBuilder.setProgress(100, progress, false);
            Notification notification = mBuilder.build();
            mNotifyManager.notify(NOTIFICATION_ID, notification);
            progressUpdateTime = System.currentTimeMillis();
            // Delegate invocation.
            onProgressUpdated(progress);
        }
    }

    protected abstract long getProgressStepDelay();

    protected abstract void onProgressUpdated(int progress);

    @Override
    protected final void onSuccess(String response) throws Throwable {
        // Closing notification.
        mNotifyManager.cancel(NOTIFICATION_ID);
        // Delegate invocation.
        onSuccessDelegate(response);
    }

    protected abstract void onSuccessDelegate(String response) throws Throwable;

    @Override
    protected final void onFail() {
        Context context = getAccountRoot().getContext();
        // When the loop is finished, updates the notification
        mBuilder.setContentText(context.getString(R.string.upload_failed))
                // Removes the progress bar
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setOngoing(false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        // Delegate invocation.
        onFailDelegate();
    }

    protected abstract void onFailDelegate();

    @Override
    protected void onCancel() {
        // Closing notification.
        mNotifyManager.cancel(NOTIFICATION_ID);
        // Delegate invocation.
        onCancelDelegate();
    }

    protected abstract void onCancelDelegate();

    @Override
    protected final void onFileNotFound() {
        onFail();
    }

    @Override
    protected final void onPending() {
        // Closing notification.
        mNotifyManager.cancel(NOTIFICATION_ID);
        // Delegate invocation.
        onPendingDelegate();
    }

    protected abstract void onPendingDelegate();
}
