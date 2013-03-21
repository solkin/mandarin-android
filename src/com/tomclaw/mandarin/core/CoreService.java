package com.tomclaw.mandarin.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 02.01.13
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */
public class CoreService extends Service {

    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;

    private static final String LOG_TAG = "MandarinLog";
    private int serviceState;
    private long serviceCreateTime;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "CoreService onCreate");
        super.onCreate();
        serviceState = STATE_DOWN;
        serviceCreateTime = System.currentTimeMillis();
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
        serviceCreateTime = 0;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "CoreService onBind");
        return new ServiceInteraction.Stub() {
            public boolean initService() throws RemoteException {
                /** Checking for service state **/
                switch (serviceState) {
                    case STATE_LOADING: {
                        return false;
                    }
                    case STATE_DOWN: {
                        CoreService.this.serviceInit();
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
    }

    /**
     * Initialize service
     */
    public void serviceInit() {
        Log.d(LOG_TAG, "CoreService serviceInit");
        serviceState = STATE_LOADING;
        // ...
        /*new Thread() {
            public void run() {
                while(true) {
                    Log.d(LOG_TAG, "CoreService [" + System.currentTimeMillis() + "]");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();*/
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        serviceState = STATE_UP;
    }

    /**
     * Returns service time from onCreate invocation
     * @return serviceCreateTime
     */
    public long getServiceCreateTime() {
        return serviceCreateTime;
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
}
