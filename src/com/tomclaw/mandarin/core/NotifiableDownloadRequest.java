package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.RangedDownloadRequest;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created by Solkin on 02.11.2014.
 */
public abstract class NotifiableDownloadRequest<A extends AccountRoot> extends RangedDownloadRequest<A> {

    private static final int NOTIFICATION_ID = 0x03;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;

    private transient long progressUpdateTime = 0;

    @Override
    protected final void onStarted() {
        Context context = getAccountRoot().getContext();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.file_download_title))
                .setContentText(getDescription())
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(0, 100, true);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        // Delegate invocation.
        onStartedDelegate();
    }

    protected abstract String getDescription();

    protected abstract void onStartedDelegate();

    @Override
    protected final void onBufferReleased(long read, long size) {
        Log.d(Settings.LOG_TAG, "downloading buffer released: " + read + "/" + size);
        final int progress = (int) (100 * read / size);
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
    protected final void onSuccess() {
        // Closing notification.
        mNotifyManager.cancel(NOTIFICATION_ID);
        // Delegate invocation.
        onSuccessDelegate();
    }

    protected abstract void onSuccessDelegate();

    @Override
    protected final void onFail() {
        Context context = getAccountRoot().getContext();
        // When the loop is finished, updates the notification
        mBuilder.setContentText(context.getString(R.string.download_failed))
                // Removes the progress bar
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
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
}
