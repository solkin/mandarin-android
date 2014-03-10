package com.tomclaw.mandarin.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;

import java.lang.ref.WeakReference;

/**
 * Created by solkin on 3/6/14.
 */
public class MusicStateReceiver extends BroadcastReceiver {

    public static final String EXTRA_MUSIC_STATUS_MESSAGE = "music_status_message";
    public static final String EXTRA_MUSIC_EVENT = "music_event";

    private static final int PROCESS_EVENT_DELAY = 1500;

    @Override
    public void onReceive(Context context, final Intent intent) {
        if(PreferenceHelper.isMusicAutoStatus(context)) {
            final WeakReference<Context> contextWeakReference = new WeakReference<Context>(context);
            MainExecutor.executeLater(new Runnable() {
                @Override
                public void run() {
                    Context context = contextWeakReference.get();
                    if (context != null) {
                        boolean isMusicActive = isMusicActive(context);
                        String action = intent.getAction();
                        String cmd = intent.getStringExtra("command");
                        String artist = intent.getStringExtra("artist");
                        String album = intent.getStringExtra("album");
                        String track = intent.getStringExtra("track");
                        Log.d(Settings.LOG_TAG, action + " / " + cmd);
                        Log.d(Settings.LOG_TAG, artist + ":" + album + ":" + track);
                        Log.d(Settings.LOG_TAG, "music active: " + isMusicActive);
                        String statusMessage = null;
                        if (!TextUtils.isEmpty(track) && isMusicActive) {
                            if(TextUtils.isEmpty(artist)) {
                                statusMessage = "";
                            } else {
                                statusMessage = artist;
                            }
                            if(!TextUtils.isEmpty(track)) {
                                if(!TextUtils.isEmpty(statusMessage)) {
                                    statusMessage = context.getString(R.string.music_status_pattern, artist, track);
                                } else {
                                    statusMessage = track;
                                }
                            }
                        }
                        sendEventToService(context, statusMessage);
                    }
                }
            }, PROCESS_EVENT_DELAY);
        }
    }

    public static void sendEventToService(Context context) {
        sendEventToService(context, null);
    }

    public static void sendEventToService(Context context, String statusMessage) {
        Intent serviceIntent = new Intent(context, CoreService.class);
        if(!TextUtils.isEmpty(statusMessage)) {
            serviceIntent.putExtra(EXTRA_MUSIC_STATUS_MESSAGE, statusMessage);
        }
        serviceIntent.putExtra(EXTRA_MUSIC_EVENT, true);
        context.startService(serviceIntent);
    }

    public static boolean isMusicActive(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isMusicActive();
    }
}