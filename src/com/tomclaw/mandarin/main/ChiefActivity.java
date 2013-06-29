package com.tomclaw.mandarin.main;

import android.app.ActivityManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.NotificationHelper;
import com.tomclaw.mandarin.core.ServiceInteraction;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 09.01.13
 * Time: 20:22
 */
public abstract class ChiefActivity extends SherlockFragmentActivity {

    private BroadcastReceiver broadcastReceiver;
    private ServiceInteraction serviceInteraction;
    private ServiceConnection serviceConnection;
    private boolean isServiceBound;
    private boolean isActivityInactive;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(Settings.LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Mandarin);
        setContentView(R.layout.progress);
        /** Starting service **/
        isServiceBound = false;
        isActivityInactive = false;
        startCoreService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityInactive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityInactive = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityInactive = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        /** Unbind **/
        unbindCoreService();
        /** Destroy **/
        super.onDestroy();
        Log.d(Settings.LOG_TAG, "ChiefActivity: onDestroy");
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
        Log.d(Settings.LOG_TAG, "bindCoreService: isServiceBound = " + isServiceBound);
        /** Checking for service is not already bound **/
        if (!isServiceBound) {
            /** Broadcast receiver **/
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("CoreService");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(Settings.LOG_TAG, "Intent in main activity received: " + intent.getStringExtra("Data"));
                    /** Checking for activity state isn't stop **/
                    if (isActivityInactive) {
                        return;
                    }
                    /** Checking for special message from service **/
                    if (intent.getBooleanExtra("Staff", false)) {
                        /** Obtain service state **/
                        int serviceState = intent.getIntExtra("State", CoreService.STATE_DOWN);
                        /** Checking for service state is up **/
                        if (serviceState == CoreService.STATE_UP) {
                            onCoreServiceReady();
                        } else if (serviceState == CoreService.STATE_DOWN) {
                            onCoreServiceDown();
                        }
                    } else {
                        /** Redirecting intent **/
                        ChiefActivity.this.onCoreServiceIntent(intent);
                    }
                }
            };
            registerReceiver(broadcastReceiver, intentFilter);
            /** Creating connection to service **/
            serviceConnection = new ServiceConnection() {
                public void onServiceDisconnected(ComponentName name) {
                    serviceInteraction = null;
                    Log.d(Settings.LOG_TAG, "onServiceDisconnected");
                }

                public void onServiceConnected(ComponentName name, IBinder service) {
                    serviceInteraction = ServiceInteraction.Stub.asInterface(service);
                    Log.d(Settings.LOG_TAG, "onServiceConnected");
                    try {
                        /** Initialize service **/
                        serviceInteraction.initService();

                        int notificationId = getIntent().getIntExtra(NotificationHelper.NOTIFICATION_ID,NotificationHelper.NOT_EXISTS_NOTIFICATION_ID);
                        if (notificationId != NotificationHelper.NOT_EXISTS_NOTIFICATION_ID){
                            serviceInteraction.clearNotifications(notificationId);
                            getIntent().removeExtra(NotificationHelper.NOTIFICATION_ID);
                        }
                    } catch (RemoteException e) {
                    }
                }
            };
            /** Binding service **/
            bindService(new Intent(this, CoreService.class), serviceConnection, BIND_AUTO_CREATE);
            isServiceBound = true;
            Log.d(Settings.LOG_TAG, "bindService completed");
        }
    }

    protected void unbindCoreService() {
        /** Checking for service is bound **/
        if (isServiceBound) {
            /** Unregister broadcast receiver **/
            unregisterReceiver(broadcastReceiver);
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
                Log.d(Settings.LOG_TAG, "checkCoreService: exist");
                return true;
            }
        }
        Log.d(Settings.LOG_TAG, "checkCoreService: none");
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
     *
     * @param intent
     */
    public abstract void onCoreServiceIntent(Intent intent);

    public final ServiceInteraction getServiceInteraction() {
        return serviceInteraction;
    }
}
