package com.tomclaw.mandarin.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;

import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.tasks.UpdateLastReadTask;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.Notifier;

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
    private AccountsDispatcher accountsDispatcher;
    private UnreadDispatcher unreadDispatcher;

    private ConnectivityReceiver connectivityReceiver;

    public static final String ACTION_CORE_SERVICE = "core_service";
    public static final String EXTRA_STAFF_PARAM = "staff";
    public static final String EXTRA_STATE_PARAM = "state";
    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;
    public static final String EXTRA_RESTART_FLAG = "restart_flag";
    public static final String EXTRA_ACTIVITY_START_EVENT = "activity_start_event";
    public static final String EXTRA_ON_CONNECTED_EVENT = "on_connected";
    public static final String EXTRA_READ_MESSAGES = "read_messages";
    public static final String EXTRA_REPLY_ON_MESSAGE = "reply_on_message";
    public static final String KEY_REPLY_ON_MESSAGE = "key_reply_on_message";

    public static final int RESTART_TIMEOUT = 5000;
    public static final int MAINTENANCE_TIMEOUT = 60000;

    private int serviceState;
    private long serviceCreateTime;
    private boolean serviceCommandReceived;
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
            Logger.log("returning " + sessionHolder.getAccountsList().size() + " accounts");
            return sessionHolder.getAccountsList();
        }

        @Override
        public void holdAccount(int accountDbId) throws RemoteException {
            Logger.log("hold account " + accountDbId);
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
        public boolean stopDownloadRequest(String tag) throws RemoteException {
            return downloadDispatcher.stopRequest(tag);
        }

        @Override
        public boolean stopUploadingRequest(String tag) throws RemoteException {
            return uploadDispatcher.stopRequest(tag);
        }
    };

    @Override
    public void onCreate() {
        long time = System.currentTimeMillis();
        Logger.log("CoreService onCreate");
        super.onCreate();
        Notifier.init(this);
        updateState(STATE_LOADING);
        serviceCreateTime = System.currentTimeMillis();
        sessionHolder = new SessionHolder(this);
        requestDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_SHORT);
        downloadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_DOWNLOAD);
        uploadDispatcher = new RequestDispatcher(this, sessionHolder, Request.REQUEST_TYPE_UPLOAD);
        accountsDispatcher = new AccountsDispatcher(this, sessionHolder);
        unreadDispatcher = new UnreadDispatcher(this);
        Logger.log("CoreService serviceInit");
        // Loading all data for this application session.
        sessionHolder.load();
        requestDispatcher.startObservation();
        downloadDispatcher.startObservation();
        uploadDispatcher.startObservation();
        accountsDispatcher.startObservation();
        unreadDispatcher.startObservation();
        // Register broadcast receivers.
        connectivityReceiver = new ConnectivityReceiver();
        registerReceiver(connectivityReceiver, connectivityReceiver.getIntentFilter());
        // Service is now ready.
        updateState(STATE_UP);
        Logger.log("CoreService serviceInit completed");
        Logger.log("core service start time: " + (System.currentTimeMillis() - time));
        // Schedule restart immediately after service creation.
        scheduleRestart(true);
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
        // Maybe, this is network availability event?
        boolean networkEvent = intent.getBooleanExtra(ConnectivityReceiver.EXTRA_NETWORK_EVENT, false);
        boolean isConnected = intent.getBooleanExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_STATUS, false);
        if (networkEvent && isConnected) {
            requestDispatcher.notifyQueue();
            downloadDispatcher.notifyQueue();
            uploadDispatcher.notifyQueue();
        }
        // Read messages event maybe?
        boolean readMessagesEvent = intent.getBooleanExtra(EXTRA_READ_MESSAGES, false);
        if (readMessagesEvent) {
            Buddy buddy = intent.getParcelableExtra(Buddy.KEY_STRUCT);
            readMessages(buddy);
        }
        // Reply on message event maybe?
        boolean replyEvent = intent.getBooleanExtra(EXTRA_REPLY_ON_MESSAGE, false);
        if (replyEvent) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                final Buddy buddy = intent.getParcelableExtra(Buddy.KEY_STRUCT);
                String text = remoteInput.getCharSequence(KEY_REPLY_ON_MESSAGE, "").toString().trim();
                sendMessage(buddy, text);
            }
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
        unregisterReceiver(connectivityReceiver);
        requestDispatcher.stopObservation();
        downloadDispatcher.stopObservation();
        uploadDispatcher.stopObservation();
        accountsDispatcher.stopObservation();
        unreadDispatcher.stopObservation();
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

    private void readMessages(Buddy buddy) {
        ContentResolver contentResolver = getContentResolver();
        Bundle bundle = new Bundle();
        bundle.putParcelable(UpdateLastReadTask.KEY_BUDDY, buddy);
        contentResolver.call(Settings.BUDDY_RESOLVER_URI,
                UpdateLastReadTask.class.getName(), null, bundle);
    }

    private void sendMessage(final Buddy buddy, String text) {
        if (!TextUtils.isEmpty(text)) {
            ChatActivity.MessageCallback callback = new ChatActivity.MessageCallback() {

                @Override
                public void onSuccess() {
                    readMessages(buddy);
                }

                @Override
                public void onFailed() {
                }
            };
            TaskExecutor.getInstance().execute(
                    new ChatActivity.SendMessageTask(this, buddy, text, callback));
        }
    }
}
