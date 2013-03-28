package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.MainActivity;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 02.01.13
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */
public class CoreService extends Service {

    private SessionHolder sessionHolder;

    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;

    private static final String LOG_TAG = "MandarinLog";
    private int serviceState;
    private long serviceCreateTime;

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
    };

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "CoreService onCreate");
        super.onCreate();
        updateState(STATE_DOWN);
        serviceCreateTime = System.currentTimeMillis();
        sessionHolder = new SessionHolder();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand flags = " + flags + " startId = " + startId);
        if ((flags & START_FLAG_REDELIVERY) == START_FLAG_REDELIVERY) {
            Log.d(LOG_TAG, "START_FLAG_REDELIVERY");
            CoreService.this.serviceInit();
        } else {
            Log.d(LOG_TAG, "Flag other");
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "CoreService onDestroy");
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
        Log.d(LOG_TAG, "CoreService onBind");
        return serviceInteraction;
    }

    /**
     * Initialize service
     */
    public void serviceInit() {
        Log.d(LOG_TAG, "CoreService serviceInit");
        updateState(STATE_LOADING);
        // ...
        new Thread() {
            public void run() {
                // Loading all data for this application session.
                sessionHolder.load();
                // For testing purposes only!
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                // Service is now ready.
                updateState(STATE_UP);
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
