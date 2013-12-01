package com.tomclaw.mandarin.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/1/13
 * Time: 3:38 PM
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(Settings.LOG_TAG, "BootCompletedReceiver onReceive " + intent.getAction());
        // Checking for autorun preference.
        if(PreferenceHelper.isAutorun(context)) {
            Log.d(Settings.LOG_TAG, "BootCompletedReceiver will now start service");
            // Starting service.
            Intent serviceIntent = new Intent(context, CoreService.class);
            context.startService(serviceIntent);
        }
    }
}
