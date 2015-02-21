package com.tomclaw.mandarin.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by solkin on 20/03/14.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    public static final String EXTRA_NETWORK_EVENT = "network_available";
    public static final String EXTRA_CONNECTIVITY_STATUS = "connectivity_status";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();

        Logger.log("connected: " + isConnected);

        sendEventToService(context, isConnected);
    }

    public static void sendEventToService(Context context, boolean isConnected) {
        Intent serviceIntent = new Intent(context, CoreService.class);
        serviceIntent.putExtra(EXTRA_NETWORK_EVENT, true);
        serviceIntent.putExtra(EXTRA_CONNECTIVITY_STATUS, isConnected);
        context.startService(serviceIntent);
    }
}
