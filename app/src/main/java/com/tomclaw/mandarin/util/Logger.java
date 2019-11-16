package com.tomclaw.mandarin.util;

import android.util.Log;

import com.tomclaw.mandarin.core.Settings;

import java.io.IOException;

import okhttp3.RequestBody;
import okio.Buffer;

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

    public static void logRequest(String message, RequestBody body) {
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            log(message + " ⟶ " + buffer.readUtf8());
        } catch (IOException ignored) {
            log("unable to log request: " + message);
        }
    }

    public static void logResponse(String message, String response) {
        log(message + " ⟵ " + response);
    }

}
