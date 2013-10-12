package com.tomclaw.mandarin.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.RequestDispatcher;

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
    private HistoryDispatcher historyDispatcher;

    public static final String ACTION_CORE_SERVICE = "core_service";
    public static final String EXTRA_STAFF_PARAM = "staff";
    public static final String EXTRA_STATE_PARAM = "state";
    public static final int STATE_DOWN = 0x00;
    public static final int STATE_LOADING = 0x01;
    public static final int STATE_UP = 0x02;

    private int serviceState;
    private long serviceCreateTime;
    private static final String appSession = String.valueOf(System.currentTimeMillis())
            .concat(String.valueOf(new Random().nextInt()));

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
    };

    @Override
    public void onCreate() {
        Log.d(Settings.LOG_TAG, "CoreService onCreate");
        super.onCreate();
        updateState(STATE_DOWN);
        serviceCreateTime = System.currentTimeMillis();
        sessionHolder = new SessionHolder(this);
        requestDispatcher = new RequestDispatcher(this, sessionHolder);
        historyDispatcher = new HistoryDispatcher(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Settings.LOG_TAG, "onStartCommand flags = " + flags + " startId = " + startId);
        if (flags == 0) {
            Log.d(Settings.LOG_TAG, "Normal service start");
            CoreService.this.serviceInit();
        } else {
            Log.d(Settings.LOG_TAG, "Flag other");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(Settings.LOG_TAG, "CoreService onDestroy");
        updateState(STATE_DOWN);
        // Reset creation time.
        serviceCreateTime = 0;
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
        // Loading all data for this application session.
        sessionHolder.load();
        requestDispatcher.startObservation();
        historyDispatcher.startObservation();
        // Service is now ready.
        updateState(STATE_UP);
        Log.d(Settings.LOG_TAG, "CoreService serviceInit completed");
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
