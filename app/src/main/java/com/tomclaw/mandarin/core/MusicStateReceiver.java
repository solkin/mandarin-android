package com.tomclaw.mandarin.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.text.TextUtils;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.Logger;

import java.lang.ref.WeakReference;

/**
 * Created by solkin on 3/6/14.
 */
public class MusicStateReceiver extends BroadcastReceiver {

    public static final String EXTRA_MUSIC_STATUS_MESSAGE = "music_status_message";
    public static final String EXTRA_MUSIC_EVENT = "music_event";

    private static final int PROCESS_EVENT_DELAY = 1500;

    private static final String[] EVENTS = new String[]{
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.android.music.playbackcomplete",
            "com.android.music.queuechanged",

            "com.sec.android.app.music.metachanged",
            "com.sec.android.app.music.playstatechanged",
            "com.sec.android.app.music.playbackcomplete",
            "com.sec.android.app.music.queuechanged",

            "com.rdio.android.metachanged",
            "com.rdio.android.playstatechanged",
            "com.rdio.android.playbackcomplete",
            "com.rdio.android.queuechanged",

            "com.andrew.apollo.metachanged",
            "com.andrew.apollo.playstatechanged",
            "com.andrew.apollo.playbackcomplete",
            "com.andrew.apollo.queuechanged",

            "com.htc.music.metachanged",
            "com.htc.music.playstatechanged",
            "com.htc.music.playbackcomplete",
            "com.htc.music.queuechanged",

            "com.miui.player.metachanged",
            "com.miui.player.playstatechanged",
            "com.miui.player.playbackcomplete",
            "com.miui.player.queuechanged",

            "com.sonyericsson.music.metachanged",
            "com.sonyericsson.music.playstatechanged",
            "com.sonyericsson.music.playbackcomplete",
            "com.sonyericsson.music.queuechanged",

            "com.samsung.sec.android.MusicPlayer.metachanged",
            "com.samsung.sec.android.MusicPlayer.playstatechanged",
            "com.samsung.sec.android.MusicPlayer.playbackcomplete",
            "com.samsung.sec.android.MusicPlayer.queuechanged"
    };

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (PreferenceHelper.isMusicAutoStatus(context)) {
            final WeakReference<Context> contextWeakReference = new WeakReference<>(context);
            MainExecutor.executeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        Context context = contextWeakReference.get();
                        if (context != null) {
                            boolean isMusicActive = isMusicActive(context);
                            String action = intent.getAction();
                            String cmd = intent.getStringExtra("command");
                            String artist = intent.getStringExtra("artist");
                            String album = intent.getStringExtra("album");
                            String track = intent.getStringExtra("track");
                            Logger.log(action + " / " + cmd);
                            Logger.log(artist + ":" + album + ":" + track);
                            Logger.log("music active: " + isMusicActive);
                            String statusMessage = null;
                            if (!TextUtils.isEmpty(track) && isMusicActive) {
                                if (TextUtils.isEmpty(artist)) {
                                    statusMessage = "";
                                } else {
                                    statusMessage = artist;
                                }
                                if (!TextUtils.isEmpty(track)) {
                                    if (!TextUtils.isEmpty(statusMessage)) {
                                        statusMessage = context.getString(R.string.music_status_pattern, artist, track);
                                    } else {
                                        statusMessage = track;
                                    }
                                }
                            }
                            sendEventToService(context, statusMessage);
                        }
                    } catch (Throwable ex) {
                        // Music event with incorrect format will be ignored.
                        Logger.log("Error while trying process music state intent", ex);
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
        if (!TextUtils.isEmpty(statusMessage)) {
            serviceIntent.putExtra(EXTRA_MUSIC_STATUS_MESSAGE, statusMessage);
        }
        serviceIntent.putExtra(EXTRA_MUSIC_EVENT, true);
        context.startService(serviceIntent);
    }

    public static boolean isMusicActive(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isMusicActive();
    }

    public IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        for (String event : EVENTS) {
            filter.addAction(event);
        }
        return filter;
    }
}
