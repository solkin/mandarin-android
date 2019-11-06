package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.DownloadCancelledException;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.ConnectivityHelper;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.preferences.PreferenceHelper;

/**
 * Created by Solkin on 02.11.2014.
 */
public abstract class NotifiableDownloadRequest<A extends AccountRoot> extends RangedDownloadRequest<A> {

    private static final int NOTIFICATION_ID = 0x03;

    private transient NotificationCompat.Builder mBuilder;
    private transient NotificationManager mNotifyManager;

    private transient long progressUpdateTime = 0;

    @Override
    protected final void onStarted() throws Throwable {
        Context context = getAccountRoot().getContext();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.file_download_title))
                .setContentText(getDescription())
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(0, 100, true)
                .setContentIntent(getIntent());
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        // Delegate invocation.
        onStartedDelegate();
    }

    protected abstract PendingIntent getIntent();

    protected boolean isStartDownload(boolean isFirstAttempt, long fileSize) {
        // Checking for this is first auto-run of this task.
        if (isFirstAttempt) {
            Context context = getAccountRoot().getContext();
            // Check for preferences of auto downloading content.
            String autoReceive = PreferenceHelper.getFilesAutoReceive(context);
            if (TextUtils.equals(autoReceive, context.getString(R.string.auto_receive_mobile_and_wi_fi))) {
                return true;
            } else if (TextUtils.equals(autoReceive, context.getString(R.string.auto_receive_mobile_less_size))) {
                return fileSize < context.getResources().getInteger(R.integer.def_auto_receive_threshold);
            } else if (TextUtils.equals(autoReceive, context.getString(R.string.auto_receive_wi_fi_only))) {
                return ConnectivityHelper.isConnectedWifi(getAccountRoot().getContext());
            } else if (TextUtils.equals(autoReceive, context.getString(R.string.auto_receive_manual_only))) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getDescription();

    protected abstract void onStartedDelegate() throws DownloadCancelledException, DownloadException;

    @Override
    protected final void onBufferReleased(long read, long size) {
        Logger.log("downloading buffer released: " + read + "/" + size);
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
    protected void onPending() {
        // Closing notification.
        mNotifyManager.cancel(NOTIFICATION_ID);
        // Delegate invocation.
        onPendingDelegate();
    }

    protected abstract void onPendingDelegate();

    @Override
    protected final void onFileNotFound() {
        onFail();
    }
}
