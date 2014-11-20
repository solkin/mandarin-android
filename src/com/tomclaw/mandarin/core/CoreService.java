package com.tomclaw.mandarin.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.Random;

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
    private HistoryDispatcher historyDispatcher;

    public static final String ACTION_CORE_SERVICE = "core_service";
    public static final String EXTRA_STAFF_PARAM = "staff";
    public static final String EXTRA_STATE_PARAM = "state";
    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;

    public static final int RESTART_TIMEOUT = 5000;

    private int serviceState;
    private long serviceCreateTime;
    private static final String appSession = String.valueOf(System.currentTimeMillis())
            .concat(String.valueOf(new Random().nextInt()));

    private ServiceInteraction.Stub serviceInteraction = new ServiceInteraction.Stub() {

        public int getServiceState() throws RemoteException {
            return serviceState;
        }

        @Override
        public long getUpTime() throws RemoteException {
            return System.currentTimeMillis() - getServiceCreateTime();
        }

        @Override
        public String getAppSession() throws RemoteException {
            return appSession;
        }

        @Override
        public List getAccountsList() throws RemoteException {
            Log.d(Settings.LOG_TAG, "returning " + sessionHolder.getAccountsList().size() + " accounts");
            return sessionHolder.getAccountsList();
        }

        @Override
        public void holdAccount(int accountDbId) throws RemoteException {
            Log.d(Settings.LOG_TAG, "hold account " + accountDbId);
            sessionHolder.holdAccountRoot(accountDbId);
        }

        @Override
        public boolean removeAccount(int accountDbId) throws RemoteException {
            return sessionHolder.removeAccountRoot(accountDbId);
        }

        @Override
        public void updateAccountStatusIndex(String accountType, String userId,
                                             int statusIndex) throws RemoteException {
            sessionHolder.updateAccountStatus(accountType, userId, statusIndex);
        }

        @Override
        public void updateAccountStatus(String accountType, String userId, int statusIndex,
                                        String statusTitle, String statusMessage) throws RemoteException {
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
        public void stopDownloadRequest(String tag) {
            downloadDispatcher.stopRequest(tag);
        }
    };

    @Override
    public void onCreate() {
        Log.d(Settings.LOG_TAG, "CoreService onCreate");
        super.onCreate();
        updateState(STATE_LOADING);
        serviceCreateTime = System.currentTimeMillis();
        sessionHolder = new SessionHolder(this);
        requestDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_SHORT);
        downloadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_DOWNLOAD);
        uploadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_UPLOAD);
        historyDispatcher = new HistoryDispatcher(this);
        Log.d(Settings.LOG_TAG, "CoreService serviceInit");
        // Loading all data for this application session.
        sessionHolder.load();
        requestDispatcher.startObservation();
        downloadDispatcher.startObservation();
        uploadDispatcher.startObservation();
        historyDispatcher.startObservation();
        // Service is now ready.
        updateState(STATE_UP);
        Log.d(Settings.LOG_TAG, "CoreService serviceInit completed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Settings.LOG_TAG, "onStartCommand flags = " + flags + " startId = " + startId);
        // Check for intent is really cool.
        if (intent != null) {
            onIntentReceived(intent);
        }
        return START_STICKY;
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
        boolean isReadMessages = intent.getBooleanExtra(HistoryDispatcher.EXTRA_READ_MESSAGES, false);
        if (isReadMessages) {
            QueryHelper.readAllMessages(getContentResolver());
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (Settings.FORCE_RESTART || Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent restartServiceIntent = new Intent(this, CoreService.class);

            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + RESTART_TIMEOUT,
                    restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(Settings.LOG_TAG, "CoreService onDestroy");
        updateState(STATE_DOWN);
        // Reset creation time.
        serviceCreateTime = 0;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Settings.LOG_TAG, "CoreService onBind");
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
}
