package com.tomclaw.mandarin.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/1/13
 * Time: 3:38 PM
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOT_EVENT = "boot_event";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Logger.log("BootCompletedReceiver onReceive " + intent.getAction());
        // Checking for autorun preference.
        if (PreferenceHelper.isAutorun(context)) {
            Logger.log("BootCompletedReceiver will now start service");
            // Starting service.
            Intent serviceIntent = new Intent(context, CoreService.class)
                    .putExtra(EXTRA_BOOT_EVENT, true);
            context.startService(serviceIntent);
        }
    }
}
