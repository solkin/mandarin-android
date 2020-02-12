package com.tomclaw.mandarin.main;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.ServiceInteraction;
import com.tomclaw.mandarin.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 09.01.13
 * Time: 20:22
 */
public abstract class ChiefActivity extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver;
    private ServiceInteraction serviceInteraction;
    private ServiceConnection serviceConnection;
    private boolean isServiceBound;
    private boolean isCoreServiceReady;
    private int themeRes;

    private List<CoreServiceListener> coreServiceListeners;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.log("ChiefActivity onCreate");

        themeRes = PreferenceHelper.getThemeRes(this);
        updateTheme();
        updateIcon();

        super.onCreate(savedInstanceState);

        coreServiceListeners = new ArrayList<>();

        setContentView(R.layout.progress);
        // Starting service.
        isServiceBound = false;
        isCoreServiceReady = false;

        startCoreService();
    }

    public void updateTheme() {
        setTheme(themeRes);
    }

    public void updateIcon() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.drawable.ic_ab_logo);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themeRes != PreferenceHelper.getThemeRes(this)) {
            Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        // Unbind
        unbindCoreService();
        // Destroy
        super.onDestroy();
        Logger.log("ChiefActivity onDestroy");
    }

    /**
     * Running core service
     */
    public void startCoreService() {
        // Checking for core service is down
        if (!checkCoreService()) {
            // Starting service
            Intent intent = new Intent(this, CoreService.class)
                    .putExtra(CoreService.EXTRA_ACTIVITY_START_EVENT, true);
            CoreService.startCoreService(this, intent);
        }
        // Bind in any case
        bindCoreService();
    }

    /**
     * Stopping core service
     */
    protected void stopCoreService() {
        // Unbind
        unbindCoreService();
        // Stop service
        Intent intent = new Intent(this, CoreService.class);
        stopService(intent);
    }

    protected void bindCoreService() {
        Logger.log("bindCoreService: isServiceBound = " + isServiceBound);
        // Checking for service is not already bound
        if (!isServiceBound) {
            // Broadcast receiver
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(CoreService.ACTION_CORE_SERVICE);
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Logger.log("Intent in main activity received: " + intent.getStringExtra("Data"));
                    // Checking for special message from service
                    if (intent.getBooleanExtra(CoreService.EXTRA_STAFF_PARAM, false)) {
                        // Obtain service state
                        int serviceState = intent.getIntExtra(CoreService.EXTRA_STATE_PARAM, CoreService.STATE_DOWN);
                        // Checking for service state is up
                        if (serviceState == CoreService.STATE_UP) {
                            coreServiceReady();
                        } else if (serviceState == CoreService.STATE_DOWN) {
                            coreServiceDown();
                        }
                    } else {
                        // Redirecting intent
                        onCoreServiceIntent(intent);
                    }
                }
            };
            registerReceiver(broadcastReceiver, intentFilter);
            // Creating connection to service
            serviceConnection = new ServiceConnection() {

                public void onServiceDisconnected(ComponentName name) {
                    serviceInteraction = null;
                    coreServiceDown();
                    Logger.log("onServiceDisconnected");
                }

                public void onServiceConnected(ComponentName name, IBinder service) {
                    serviceInteraction = ServiceInteraction.Stub.asInterface(service);
                    coreServiceReady();
                    Logger.log("onServiceConnected");
                }
            };
            // Binding service
            bindService(new Intent(this, CoreService.class), serviceConnection, BIND_AUTO_CREATE);
            isServiceBound = true;
            Logger.log("bindService completed");
        }
    }

    protected void unbindCoreService() {
        // Checking for service is bound
        if (isServiceBound) {
            // Unregister broadcast receiver
            unregisterReceiver(broadcastReceiver);
            // Unbind service
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
        List<ActivityManager.RunningServiceInfo> runningServiceInfoList = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfoList != null) {
            for (ActivityManager.RunningServiceInfo service : runningServiceInfoList) {
                if (CoreService.class.getCanonicalName().equals(service.service.getClassName())) {
                    Logger.log("checkCoreService: exist");
                    return true;
                }
            }
        }
        Logger.log("checkCoreService: none");
        return false;
    }

    /**
     * Checks for core service state is changed and if it really changed, invokes inCoreServiceReady.
     */
    private void coreServiceReady() {
        if (!isCoreServiceReady) {
            isCoreServiceReady = true;
            notifyCoreServiceReady();
        }
    }

    /**
     * Checks for core service state is changed and if it really changed, invokes onCoreServiceDown.
     */
    private void coreServiceDown() {
        if (isCoreServiceReady) {
            isCoreServiceReady = false;
            notifyCoreServiceDown();
        }
    }

    /**
     * Returns current service state
     *
     * @return boolean - service state
     */
    public boolean isCoreServiceReady() {
        return isCoreServiceReady;
    }

    /**
     * Any message from service for this activity
     *
     * @param intent from service to activity.
     */
    public abstract void onCoreServiceIntent(Intent intent);

    public final ServiceInteraction getServiceInteraction() {
        return serviceInteraction;
    }

    public void addCoreServiceListener(CoreServiceListener listener) {
        coreServiceListeners.add(listener);
    }

    public void removeCoreServiceListener(CoreServiceListener listener) {
        coreServiceListeners.remove(listener);
    }

    private void notifyCoreServiceReady() {
        for (CoreServiceListener listener : coreServiceListeners) {
            listener.onCoreServiceReady();
        }
    }

    private void notifyCoreServiceDown() {
        for (CoreServiceListener listener : coreServiceListeners) {
            listener.onCoreServiceDown();
        }
    }

    public interface CoreServiceListener {

        /**
         * Activity notification, service if now ready
         */
        void onCoreServiceReady();

        /**
         * Activity notification, service going down
         */
        void onCoreServiceDown();
    }
}
