package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.RequestDispatcher;
import com.tomclaw.mandarin.main.MainActivity;

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
    private NotificationHelper notificationHelper;
    private BroadcastReceiver notificationsReceiver;

    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;

    private int serviceState;
    private long serviceCreateTime;
    private static final String appSession = String.valueOf(System.currentTimeMillis())
            .concat(String.valueOf(new Random().nextInt()));

    /**
     * For showing and hiding our notification.
     */
    NotificationManager notificationManager;

    private ServiceInteraction.Stub serviceInteraction = new ServiceInteraction.Stub() {
        public boolean initService() throws RemoteException {
            /** Checking for service state **/
            switch (serviceState) {
                case STATE_LOADING: {
                    return false;
                }
                case STATE_DOWN: {
                    CoreService.this.serviceInit();
                    return false;
                }
                case STATE_UP: {
                    sendState();
                    return true;
                }
                default: {
                    /** What the fuck? **/
                    return false;
                }
            }
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
        public void addAccount(CoreObject coreObject) throws RemoteException {
            AccountRoot accountRoot = (AccountRoot) coreObject;
            Log.d(Settings.LOG_TAG, "add " + accountRoot.getUserId() + " account");
            sessionHolder.updateAccountRoot(accountRoot);
        }

        @Override
        public boolean removeAccount(String accountType, String userId) throws RemoteException {
            return sessionHolder.removeAccountRoot(accountType, userId);
        }

        @Override
        public void updateAccountStatus(String accountType, String userId, int statusIndex) throws RemoteException {
            sessionHolder.updateAccountStatus(accountType, userId, statusIndex);
        }

        @Override
        public void clearNotifications(int id) throws RemoteException {
            notificationHelper.clearNotifications(id);
        }
    };

    @Override
    public void onCreate() {
        Log.d(Settings.LOG_TAG, "CoreService onCreate");
        super.onCreate();
        updateState(STATE_DOWN);
        serviceCreateTime = System.currentTimeMillis();
        sessionHolder = new SessionHolder(this);
        requestDispatcher = new RequestDispatcher(this);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationHelper = new NotificationHelper(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("Notifications");
        notificationsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int id = intent.getIntExtra("id", -1);
                if (id != -1) {
                    notificationHelper.clearNotifications(id);
                }
            }
        };
        registerReceiver(notificationsReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Settings.LOG_TAG, "onStartCommand flags = " + flags + " startId = " + startId);
        if ((flags & START_FLAG_REDELIVERY) == START_FLAG_REDELIVERY) {
            Log.d(Settings.LOG_TAG, "START_FLAG_REDELIVERY");
            CoreService.this.serviceInit();
        } else {
            Log.d(Settings.LOG_TAG, "Flag other");
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(Settings.LOG_TAG, "CoreService onDestroy");
        updateState(STATE_DOWN);
        // Reset creation time.
        serviceCreateTime = 0;
        // Cancel the persistent notification.
        notificationManager.cancel(R.string.app_name);
        // Tell the user we stopped.
        Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Settings.LOG_TAG, "CoreService onBind");
        return serviceInteraction;
    }

    /**
     * Initialize service
     */
    public void serviceInit() {
        Log.d(Settings.LOG_TAG, "CoreService serviceInit");
        updateState(STATE_LOADING);
        // ...
        new Thread() {
            public void run() {
                // Loading all data for this application session.
                sessionHolder.load();
                // For testing purposes only!
                try {
                    Thread.sleep(1000);
                    notificationHelper.createMessageNotification(2, "Mandarin", "Hello!");
                    notificationHelper.createMessageNotification(2, "Mandarin", "What's up?");
                    notificationHelper.createMessageNotification(1, "Mandarin", "1");
                    notificationHelper.createMessageNotification(1, "Mandarin", "2");
                    notificationHelper.createMessageNotification(1, "Mandarin", "3");
                    //showNotification();
                    Log.d(Settings.LOG_TAG, "notification");
                } catch (InterruptedException ignored) {
                }
                // Service is now ready.
                updateState(STATE_UP);

                try {
                    Thread.sleep(20000);
                    notificationHelper.createMessageNotification(2, "Mandarin", "Bye!");
                    //showNotification();
                    Log.d(Settings.LOG_TAG, "notification");
                } catch (InterruptedException ignored) {
                }
            }
        }.start();

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
        Intent intent = new Intent("CoreService");
        intent.putExtra("Staff", true);
        intent.putExtra("State", serviceState);
        sendBroadcast(intent);
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.app_name);
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                text, contentIntent);
        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        notificationManager.notify(R.string.app_name, notification);
    }
}
