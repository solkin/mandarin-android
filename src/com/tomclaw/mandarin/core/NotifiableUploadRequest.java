package com.tomclaw.mandarin.core;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created by Solkin on 21.10.2014.
 */
public abstract class NotifiableUploadRequest<A extends AccountRoot> extends RangedUploadRequest<A> {

    private static final int NOTIFICATION_ID = 0x02;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;

    private transient int progressShown = 0;

    @Override
    protected final void onStarted() throws Throwable {
        Context context = getAccountRoot().getContext();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.file_upload_title))
                .setContentText(getDescription())
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setLargeIcon(getLargeIcon())
                .setOngoing(true);
        // Delegate invocation.
        onStartedDelegate();
    }

    protected abstract String getDescription();

    protected abstract Bitmap getLargeIcon();

    protected abstract void onStartedDelegate() throws Throwable;

    @Override
    protected final void onBufferReleased(long sent, long size) {
        int progress = (int) (100 * sent / size);
        if ((progressShown == 0 && progress > 0) || (progress - progressShown) > getProgressStep()) {
            mBuilder.setProgress(100, progress, false);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            progressShown = progress;
            // Delegate invocation.
            onProgressUpdated(progress);
        }
    }

    protected abstract int getProgressStep();

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
    protected final void onFileNotFound() {
        onFail();
    }
}
