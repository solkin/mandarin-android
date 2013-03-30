package com.tomclaw.mandarin.main;

import android.app.ActivityManager;
import android.content.*;
import android.os.*;
import android.util.Log;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 09.01.13
 * Time: 20:22
 * To change this template use File | Settings | File Templates.
 */
public abstract class ChiefActivity extends SherlockFragmentActivity {

    public static final String LOG_TAG = "MandarinLog";

    private ServiceConnection serviceConnection;
    protected boolean isServiceBound;

    /*Messenger for communicating with the service*/
    protected Messenger activityMessenger = null;
    /* Mesenger который мы отдаем сервису для взаимодействия с активити */
    protected Messenger serviceMessenger = new Messenger(new ChiefActivityHandler());

    class ChiefActivityHandler extends Handler {
        public void handleMessage(Message msg){
            Bundle bundle;
            switch (msg.what){
                case CoreService.GET_UPTIME:
                    bundle = msg.getData();
                    long time = bundle.getLong("time");
                    Log.d(LOG_TAG, "Received time = " + time);
                    break;
                case CoreService.STATE:
                    Log.d(LOG_TAG, "State in main activity received ");
                    bundle = msg.getData();
                    int serviceState = bundle.getInt("State", CoreService.STATE_DOWN);
                    /** Checking for service state is up **/
                    if (serviceState == CoreService.STATE_UP) {
                        onCoreServiceReady();
                    } else if (serviceState == CoreService.STATE_DOWN) {
                        onCoreServiceDown();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Mandarin);
        setContentView(R.layout.progress);
        /** Starting service **/
        isServiceBound = false;
        startCoreService();
    }


    @Override
    protected void onDestroy() {
        /** Unbind **/
        unbindCoreService();
        /** Destroy **/
        super.onDestroy();
        Log.d(LOG_TAG, "ChiefActivity: onDestroy");
    }

    /**
     * Running core service
     */
    protected void startCoreService() {
        /** Checking for core service is down **/
        if (!checkCoreService()) {
            /** Starting service **/
            Intent intent = new Intent(this, CoreService.class);
            startService(intent);
        }
        /** Bind in any case **/
        bindCoreService();
    }

    /**
     * Stopping core service
     */
    protected void stopCoreService() {
        /** Unbind **/
        unbindCoreService();
        /** Stop service **/
        Intent intent = new Intent(this, CoreService.class);
        stopService(intent);
    }

    protected void bindCoreService() {
        Log.d(LOG_TAG, "bindCoreService: isServiceBound = " + isServiceBound);
        /** Checking for service is not already bound **/
        if (!isServiceBound) {
            /** Broadcast receiver **/
            /*IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("CoreServiLce");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(LOG_TAG, "Intent in main activity received: " + intent.getStringExtra("Data"));
                    *//** Checking for special message from service **//*
                    if (intent.getBooleanExtra("Staff", false)) {
                        *//** Obtain service state **//*
                        int serviceState = intent.getIntExtra("State", CoreService.STATE_DOWN);
                        *//** Checking for service state is up **//*
                        if (serviceState == CoreService.STATE_UP) {
                            onCoreServiceReady();
                        } else if (serviceState == CoreService.STATE_DOWN) {
                            onCoreServiceDown();
                        }
                    } else {
                        *//** Redirecting intent **//*
                        ChiefActivity.this.onCoreServiceIntent(intent);
                    }
                }
            };
            registerReceiver(broadcastReceiver, intentFilter);*/
            /** Creating connection to service **/
            serviceConnection = new ServiceConnection() {
                public void onServiceDisconnected(ComponentName name) {
                    //serviceInteraction = null;
                    activityMessenger = null;
                    isServiceBound = false;
                    Log.d(LOG_TAG, "onServiceDisconnected");
                }

                public void onServiceConnected(ComponentName name, IBinder service) {
                    //serviceInteraction = ServiceInteraction.Stub.asInterface(service);
                    activityMessenger = new Messenger(service);
                    Log.d(LOG_TAG, "onServiceConnected");
                    isServiceBound = true;

                    Message msg = Message.obtain(null, CoreService.INIT_STATE);
                    msg.replyTo = serviceMessenger;
                    try {
                        if(activityMessenger != null) {
                            Log.d(LOG_TAG, "send init to service");
                            activityMessenger.send(msg);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            };
            /** Binding service **/
            bindService(new Intent(this, CoreService.class), serviceConnection, BIND_AUTO_CREATE);
            //isServiceBound = true;
            Log.d(LOG_TAG, "bindService completed");
        }
    }

    protected void unbindCoreService() {
        /** Checking for service is bound **/
        if (isServiceBound) {
            /** Unregister broadcast receiver **/
            //unregisterReceiver(broadcastReceiver);
            /** Unbind service **/
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    /**
     * Checking for core service is running
     *
     * @return core service status
     */
    protected boolean checkCoreService() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CoreService.class.getCanonicalName().equals(service.service.getClassName())) {
                Log.d(LOG_TAG, "checkCoreService: exist");
                return true;
            }
        }
        Log.d(LOG_TAG, "checkCoreService: none");
        return false;
    }

    /**
     * Activity notification, service if now ready
     */
    public abstract void onCoreServiceReady();

    /**
     * Activity notification, service going down
     */
    public abstract void onCoreServiceDown();

    /**
     * Any message from service for this activity
     * @param intent
     */
    public abstract void onCoreServiceIntent(Intent intent);

    /*public final ServiceInteraction getServiceInteraction() {
        return serviceInteraction;
    }*/
}
