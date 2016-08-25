package com.tomclaw.mandarin.util;

import android.util.Log;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created by Solkin on 07.02.2015.
 */
public class Logger {

    public static void log(String message) {
        Log.d(Settings.LOG_TAG, message);
    }

    public static void logWithPrefix(String prefix, String message) {
        Log.d(Settings.LOG_TAG, "[" + prefix + "] " + message);
    }

    public static void log(String message, Throwable ex) {
        Log.d(Settings.LOG_TAG, message, ex);
    }
}
