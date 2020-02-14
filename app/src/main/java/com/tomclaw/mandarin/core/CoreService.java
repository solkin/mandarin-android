package com.tomclaw.mandarin.core;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.Notifier;

import java.util.Random;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 02.01.13
 * Time: 18:48
 */
public class CoreService extends Service {

    private SessionHolder sessionHolder;
    private RequestDispatcher requestDispatcher;
    private RequestDispatcher downloadDispatcher;
    private RequestDispatcher uploadDispatcher;
    @SuppressWarnings("FieldCanBeLocal")
    private HistoryDispatcher historyDispatcher;
    @SuppressWarnings("FieldCanBeLocal")
    private AccountsDispatcher accountsDispatcher;

    private static final int NOTIFICATION_ID = 0x42;
    private static final String CHANNEL_ID = "core_service";

    public static final String ACTION_CORE_SERVICE = "core_service";
    public static final String EXTRA_STAFF_PARAM = "staff";
    public static final String EXTRA_STATE_PARAM = "state";
    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;
    public static final String EXTRA_RESTART_FLAG = "restart_flag";
    public static final String EXTRA_ACTIVITY_START_EVENT = "activity_start_event";
    public static final String EXTRA_ON_CONNECTED_EVENT = "on_connected";
    public static final String EXTRA_ON_DISCONNECTED_EVENT = "on_disconnected";

    public static final int RESTART_TIMEOUT = 5000;
    public static final int MAINTENANCE_TIMEOUT = 60000;

    private int serviceState;
    private long serviceCreateTime;
    private boolean serviceCommandReceived;
    private static final String appSession = String.valueOf(System.currentTimeMillis())
            .concat(String.valueOf(new Random().nextInt()));

    private ServiceInteraction.Stub serviceInteraction = new ServiceInteraction.Stub() {

        public int getServiceState() {
            return serviceState;
        }

        @Override
        public long getUpTime() {
            return System.currentTimeMillis() - getServiceCreateTime();
        }

        @Override
        public String getAppSession() {
            return appSession;
        }

        @Override
        public void holdAccount(int accountDbId) {
            Logger.log("hold account " + accountDbId);
            sessionHolder.holdAccountRoot(accountDbId);
        }

        @Override
        public boolean removeAccount(int accountDbId) {
            return sessionHolder.removeAccountRoot(accountDbId);
        }

        @Override
        public void updateAccountStatusIndex(String accountType, String userId, int statusIndex) {
            sessionHolder.updateAccountStatus(accountType, userId, statusIndex);
        }

        @Override
        public void updateAccountStatus(String accountType, String userId, int statusIndex,
                                        String statusTitle, String statusMessage) {
            sessionHolder.updateAccountStatus(accountType, userId, statusIndex, statusTitle, statusMessage);
        }

        @Override
        public void connectAccounts() {
            sessionHolder.connectAccounts();
        }

        @Override
        public void disconnectAccounts() {
            sessionHolder.disconnectAccounts();
        }

        @Override
        public boolean stopDownloadRequest(String tag) {
            return downloadDispatcher.stopRequest(tag);
        }

        @Override
        public boolean stopUploadingRequest(String tag) {
            return uploadDispatcher.stopRequest(tag);
        }
    };

    @Override
    public void onCreate() {
        long time = System.currentTimeMillis();
        Logger.log("CoreService onCreate");
        super.onCreate();
        updateState(STATE_LOADING);
        serviceCreateTime = System.currentTimeMillis();
        Notifier.init(this);
        sessionHolder = new SessionHolder(this);
        requestDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_SHORT);
        downloadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_DOWNLOAD);
        uploadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_UPLOAD);
        historyDispatcher = new HistoryDispatcher(this);
        accountsDispatcher = new AccountsDispatcher(this, sessionHolder);
        Logger.log("CoreService serviceInit");
        // Loading all data for this application session.
        sessionHolder.load();
        requestDispatcher.startObservation();
        downloadDispatcher.startObservation();
        uploadDispatcher.startObservation();
        historyDispatcher.startObservation();
        accountsDispatcher.startObservation();
        // Register broadcast receivers.
        MusicStateReceiver musicStateReceiver = new MusicStateReceiver();
        registerReceiver(musicStateReceiver, musicStateReceiver.getIntentFilter());
        ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
        registerReceiver(connectivityReceiver, connectivityReceiver.getIntentFilter());
        // Service is now ready.
        updateState(STATE_UP);
        Logger.log("CoreService serviceInit completed");
        Logger.log("core service start time: " + (System.currentTimeMillis() - time));
        startForeground();
        // Schedule restart immediately after service creation.
        scheduleRestart(true);
    }

    private void startForeground() {
        Handler handler = new Handler();
        if (isForegroundService()) {
            final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
                final Runnable callback = new Runnable() {
                    @Override
                    public void run() {
                        String name = getString(R.string.core_service);
                        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MIN);
                        notificationChannel.setShowBadge(false);
                        notificationManager.createNotificationChannel(notificationChannel);
                    }
                };
                callback.run();
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
                final Notification notification = buildNotification(builder);
                startForeground(NOTIFICATION_ID, notification);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        callback.run();
                        try {
                            notificationManager.notify(NOTIFICATION_ID, notification);
                        } catch (Throwable ignored) {
                        }
                    }
                };
                runnable.run();
                handler.post(runnable);
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "");
                Notification notification = buildNotification(builder);
                startForeground(NOTIFICATION_ID, notification);
            }
        }
        if (!sessionHolder.hasActiveAccounts()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    stopForeground(true);
                }
            };
            handler.post(runnable);
        }
    }

    private Notification buildNotification(NotificationCompat.Builder builder) {
        String title = getString(R.string.foreground_title);
        String subtitle = getString(R.string.foreground_description);
        int color = getResources().getColor(R.color.accent_color);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return builder
                .setContentTitle(title)
                .setContentText(subtitle)
                .setColor(color)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.log("onStartCommand flags = " + flags + " startId = " + startId);
        // Check for intent is really cool.
        if (intent != null) {
            boolean restartEvent = intent.getBooleanExtra(EXTRA_RESTART_FLAG, false);
            if (serviceCommandReceived && restartEvent) {
                Logger.logWithPrefix("W", "Received extra start command restart event.");
                scheduleRestart(true);
            } else {
                onIntentReceived(intent);
                serviceCommandReceived = true;
            }
        }
        return START_NOT_STICKY;
    }

    private void onIntentReceived(Intent intent) {
        // Parse music event info.
        boolean musicEvent = intent.getBooleanExtra(MusicStateReceiver.EXTRA_MUSIC_EVENT, false);
        // Checking for this is music event and we must process fresh data or
        // music is not longer playing and we must reset auto status.
        if (musicEvent || !MusicStateReceiver.isMusicActive(this)) {
            String statusMessage = intent.getStringExtra(MusicStateReceiver.EXTRA_MUSIC_STATUS_MESSAGE);
            if (!TextUtils.isEmpty(statusMessage)) {
                sessionHolder.setAutoStatus(statusMessage);
            } else {
                sessionHolder.resetAutoStatus();
            }
        }
        // Maybe, this is network availability event?
        boolean networkEvent = intent.getBooleanExtra(ConnectivityReceiver.EXTRA_NETWORK_EVENT, false);
        boolean isConnected = intent.getBooleanExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_STATUS, false);
        if (networkEvent && isConnected) {
            requestDispatcher.notifyQueue();
            downloadDispatcher.notifyQueue();
            uploadDispatcher.notifyQueue();
        }
        // Read messages event maybe?
        boolean readMessagesEvent = intent.getBooleanExtra(HistoryDispatcher.EXTRA_READ_MESSAGES, false);
        if (readMessagesEvent) {
            QueryHelper.readAllMessages(getContentResolver());
        }
        // Or maybe this is after-boot event?
        boolean bootEvent = intent.getBooleanExtra(BootCompletedReceiver.EXTRA_BOOT_EVENT, false);
        // Checking for this is boot event and no any active account.
        if (bootEvent && !sessionHolder.hasActiveAccounts()) {
            Logger.log("Service started after device boot, but no active accounts. Stopping self.");
            stopSelf();
            System.exit(0);
        }
        // Check for this is connection event from accounts dispatcher.
        boolean connectedEvent = intent.getBooleanExtra(EXTRA_ON_CONNECTED_EVENT, false);
        if (connectedEvent) {
            Logger.logWithPrefix("W", "Received account connected event. Scheduling restart.");
            scheduleRestart(true);
            startForeground();
        }
        // Check for this is disconnection event from accounts dispatcher.
        boolean disconnectedEvent = intent.getBooleanExtra(EXTRA_ON_DISCONNECTED_EVENT, false);
        if (disconnectedEvent) {
            Logger.logWithPrefix("W", "Received accounts disconnected event.");
            stopForeground(true);
        }
        // Check for service restarted automatically while there is no connected accounts.
        boolean restartEvent = intent.getBooleanExtra(EXTRA_RESTART_FLAG, false);
        if (restartEvent) {
            Logger.logWithPrefix("W", "Service was restarted automatically.");
            if (!sessionHolder.hasActiveAccounts()) {
                Logger.logWithPrefix("W", "No active accounts. Stopping self.");
                stopSelf();
                System.exit(0);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        scheduleRestart(false);
        super.onTaskRemoved(rootIntent);
    }

    private void scheduleRestart(boolean maintenance) {
        // Schedule service restarting.
        Logger.logWithPrefix("W", "Attempting to schedule restart.");
        if (Settings.FORCE_RESTART && sessionHolder.hasActiveAccounts()) {
            Intent restartServiceIntent = new Intent(this, CoreService.class)
                    .putExtra(EXTRA_RESTART_FLAG, true);
            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + (maintenance ? MAINTENANCE_TIMEOUT : RESTART_TIMEOUT),
                    restartServicePendingIntent);
            Logger.logWithPrefix("W", "Restart scheduled.");
        }
    }

    @Override
    public void onDestroy() {
        Logger.log("CoreService onDestroy");
        updateState(STATE_DOWN);
        // Reset creation time.
        serviceCreateTime = 0;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.log("CoreService onBind");
        return serviceInteraction;
    }

    /**
     * Returns service time from onCreate invocation
     *
     * @return serviceCreateTime
     */
    public long getServiceCreateTime() {
        return serviceCreateTime;
    }

    /**
     * Returns application session id. Every service restart will cause session change.
     *
     * @return appSession
     */
    public static String getAppSession() {
        return appSession;
    }

    public void updateState(int serviceState) {
        this.serviceState = serviceState;
        sendState();
    }

    /**
     * Sending state to broadcast
     */
    private void sendState() {
        Intent intent = new Intent(ACTION_CORE_SERVICE);
        intent.putExtra(EXTRA_STAFF_PARAM, true);
        intent.putExtra(EXTRA_STATE_PARAM, serviceState);
        sendBroadcast(intent);
    }

    public static void startCoreService(Context context, Intent intent) {
        if (isForegroundService()) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static boolean isForegroundService() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}
