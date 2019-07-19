package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.tasks.UpdateNotificationTask;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.preferences.PreferenceHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by solkin on 05.07.17.
 */
public class UnreadDispatcher {

    private static final long BUDDY_DISPATCH_DELAY = 750;

    private Context context;
    private ContentResolver contentResolver;
    private ContentObserver unreadObserver;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private boolean privateNotifications, settingsChanged;

    public UnreadDispatcher(Context context) {
        this.context = context;
        contentResolver = context.getContentResolver();
        unreadObserver = new UnreadObserver();
    }

    public void startObservation() {
        contentResolver.registerContentObserver(Settings.BUDDY_RESOLVER_URI, true, unreadObserver);

        unreadObserver.onChange(true);

        observePreferences();
    }

    private void observePreferences() {
        // Observing notification preferences to immediately update current notification.
        privateNotifications = PreferenceHelper.isPrivateNotifications(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (TextUtils.equals(key, context.getString(R.string.pref_private_notifications))) {
                    boolean privateNotifications = PreferenceHelper.isPrivateNotifications(context);
                    if (UnreadDispatcher.this.privateNotifications != privateNotifications) {
                        UnreadDispatcher.this.privateNotifications = privateNotifications;
                        settingsChanged = true;
                        unreadObserver.onChange(true);
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private class UnreadObserver extends ContentObserver {

        ExecutorService executor;
        Runnable taskWrapper;

        UnreadObserver() {
            super(null);
            executor = Executors.newSingleThreadExecutor();
            taskWrapper = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(BUDDY_DISPATCH_DELAY);
                    } catch (InterruptedException ignored) {
                    }
                    Bundle bundle = new Bundle();
                    contentResolver.call(Settings.BUDDY_RESOLVER_URI,
                            UpdateNotificationTask.class.getName(), null, bundle);
                }
            };
        }

        @Override
        public void onChange(boolean selfChange) {
            Logger.log("UnreadObserver: onChange [selfChange = " + selfChange + "]");
            executor.submit(taskWrapper);
        }
    }
}
