package com.tomclaw.mandarin.util;

import android.annotation.SuppressLint;
import android.util.Log;

import com.tomclaw.mandarin.core.Settings;

import java.io.IOException;

import okhttp3.RequestBody;
import okio.Buffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * Created by Solkin on 07.02.2015.
 */
public class Logger {

    private static class Holder {

        static Logger instance = new Logger();
    }

    public static Logger getInstance() {
        return Holder.instance;
    }

    private PrintStream stream;

    @SuppressLint("SimpleDateFormat")
    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy / HH:mm");

    private Logger() {
        if (Settings.LOG_TO_FILE) {
            try {
                File dir = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                if (dir.exists() || dir.mkdirs()) {
                    File file = new File(dir, "mandarin.log");
                    stream = new PrintStream(new FileOutputStream(file, true));
                    header();
                    return;
                }
            } catch (FileNotFoundException ignored) {
            }
        }
        stream = new PrintStream(new LogOutputStream());
    }

    private void header() {
        String date = dateFormat.format(System.currentTimeMillis());
        stream.println();
        stream.println();
        stream.print("--- " + date + " ---");
        stream.println();
        stream.println();
        stream.flush();
    }

    private void print(String message) {
        stream.println(System.currentTimeMillis() + ": " + message);
        stream.flush();
    }

    private void print(String message, Throwable ex) {
        print(message + '\n' + ex.toString());
    }

    public static void log(String message) {
        Logger.getInstance().print(message);
    }

    public static void logWithPrefix(String prefix, String message) {
        Logger.getInstance().print("[" + prefix + "] " + message);
    }

    public static void log(String message, Throwable ex) {
        Logger.getInstance().print(message, ex);
    }

    private class LogOutputStream extends OutputStream {

        private StringBuilder buf = new StringBuilder();

        @Override
        public void write(int b) {
            buf.append((char) b);
            if (b == '\n') {
                Log.d(Settings.LOG_TAG, buf.toString());
                buf.setLength(0);
                buf.trimToSize();
            }
        }
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
