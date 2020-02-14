package com.tomclaw.mandarin.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.NotificationContent;
import com.tomclaw.mandarin.util.NotificationLine;
import com.tomclaw.mandarin.util.Notifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 28.09.13
 * Time: 15:40
 */
public class HistoryDispatcher {

    private static final long HISTORY_DISPATCH_DELAY = 750;

    static String EXTRA_READ_MESSAGES = "read_messages";

    private Context context;
    private NotificationManager notificationManager;
    private ContentResolver contentResolver;
    private ContentObserver historyObserver;
    private volatile long notificationCancelTime = 0;

    private static final int NOTIFICATION_ID = 0x01;
    private static final String NOTIFICATION_CHANNEL_ID = "messages";

    private final int largeIconSize;
    private final int previewSize;

    private boolean privateNotifications, settingsChanged;

    public HistoryDispatcher(Context context) {
        // Variables.
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        contentResolver = context.getContentResolver();
        // Creating observers.
        historyObserver = new HistoryObserver();
        largeIconSize = BitmapCache.convertDpToPixel(64, context);
        previewSize = BitmapCache.BITMAP_SIZE_ORIGINAL;
    }

    private void initChannel() {
        Notifier.init(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
            if (channel != null) {
                updateNotificationChannel(channel);
            } else {
                CharSequence channelName = context.getString(R.string.incoming_messages);
                channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        channelName,
                        IMPORTANCE_DEFAULT
                );
                updateNotificationChannel(channel);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateNotificationChannel(NotificationChannel channel) {
        Uri sound = PreferenceHelper.getNotificationUri(context);
        channel.setShowBadge(true);
        channel.setSound(sound, RingtoneManager.getRingtone(context, sound).getAudioAttributes());
        channel.enableLights(true);
        channel.setLightColor(context.getResources().getColor(R.color.accent_color));
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 100, 0, 100});
    }

    public void startObservation() {
        // Registering created observers.
        contentResolver.registerContentObserver(
                Settings.HISTORY_RESOLVER_URI, true, historyObserver);

        historyObserver.onChange(true);

        observePreferences();
    }

    private void observePreferences() {
        // Observing notification preferences to immediately update current notification.
        privateNotifications = PreferenceHelper.isPrivateNotifications(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (TextUtils.equals(key, context.getString(R.string.pref_private_notifications))) {
                    boolean privateNotifications = PreferenceHelper.isPrivateNotifications(context);
                    if (HistoryDispatcher.this.privateNotifications != privateNotifications) {
                        HistoryDispatcher.this.privateNotifications = privateNotifications;
                        settingsChanged = true;
                        historyObserver.onChange(true);
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private class HistoryObserver extends ContentObserver {

        ExecutorService executor;
        HistoryDispatcherTask historyDispatcherTask;
        Runnable taskWrapper;

        /**
         * Creates a content observer.
         */
        public HistoryObserver() {
            super(null);
            executor = Executors.newSingleThreadExecutor();
            historyDispatcherTask = new HistoryDispatcherTask();
            taskWrapper = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(HISTORY_DISPATCH_DELAY);
                    } catch (InterruptedException ignored) {
                    }
                    TaskExecutor.getInstance().execute(historyDispatcherTask);
                }
            };
        }

        @Override
        public void onChange(boolean selfChange) {
            Logger.log("HistoryObserver: onChange [selfChange = " + selfChange + "]");
            executor.submit(taskWrapper);
        }
    }

    private class HistoryDispatcherTask extends Task {

        @Override
        @SuppressWarnings("unchecked")
        public void executeBackground() throws Throwable {
            long time = System.currentTimeMillis();
            // Obtain last unread for buddy. If exist.
            Bundle bundle = contentResolver.call(Settings.HISTORY_RESOLVER_URI, GlobalProvider.METHOD_GET_MESSAGES_COUNT, null, null);
            int unshown = bundle.getInt(GlobalProvider.KEY_UNSHOWN);
            int justShown = bundle.getInt(GlobalProvider.KEY_JUST_SHOWN);
            int onScreen = bundle.getInt(GlobalProvider.KEY_ON_SCREEN);
            // Checking for non-shown messages exist.
            // If yes - we must update notification with all unread messages. If no - nothing to do now.
            if (unshown > 0 || justShown > 0 || onScreen > 0 || settingsChanged) {
                bundle = contentResolver.call(Settings.HISTORY_RESOLVER_URI, GlobalProvider.METHOD_GET_UNREAD, null, null);
                ArrayList<NotificationData> unreadList =
                        (ArrayList<NotificationData>) bundle.getSerializable(GlobalProvider.KEY_NOTIFICATION_DATA);
                // Checking for unread messages exist. If no, we must cancel notification.
                if (unreadList != null && !unreadList.isEmpty()) {
                    boolean isAlarmRequired = (unshown > 0);
                    // Building variables.
                    int unread = 0;
                    // Last-message variables.
                    int buddyDbId;
                    Bitmap largeIcon = null;
                    String message;
                    int contentType;
                    int requestCode = 1;
                    List<NotificationLine> lines = new ArrayList<>();
                    for (NotificationData data : unreadList) {
                        // Obtaining and collecting message-specific data.
                        unread += data.getUnreadCount();
                        message = data.getMessageText();
                        buddyDbId = data.getBuddyDbId();
                        String nickName = data.getBuddyNick();
                        String avatarHash = data.getBuddyAvatarHash();
                        contentType = data.getContentType();
                        if (TextUtils.isEmpty(nickName)) {
                            nickName = context.getString(R.string.unknown_buddy);
                            avatarHash = null;
                        }
                        if (!TextUtils.isEmpty(avatarHash)) {
                            // Obtain avatar for notification.
                            largeIcon = BitmapCache.getInstance().getBitmapSync(
                                    avatarHash, largeIconSize, largeIconSize, true, true);
                            // Make round avatar for lollipop and newer.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                largeIcon = BitmapHelper.getRoundBitmap(largeIcon);
                            }
                        }

                        PendingIntent openChatIntent = createOpenChatIntent(context, buddyDbId, requestCode++);
                        PendingIntent replyIntent = createReplyIntent(context, buddyDbId, requestCode++);
                        PendingIntent readIntent = createReadIntent(context, buddyDbId, requestCode++);

                        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                                R.drawable.ic_reply, context.getString(R.string.reply_now), replyIntent)
                                .setAllowGeneratedReplies(true)
                                .build();
                        NotificationCompat.Action readAction = new NotificationCompat.Action.Builder(
                                R.drawable.ic_action_read, context.getString(R.string.mark_as_read), readIntent)
                                .setAllowGeneratedReplies(true)
                                .build();

                        String text;
                        switch (contentType) {
                            case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE:
                                text = "Photo";
                                break;
                            case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                                text = "Video";
                                break;
                            case GlobalProvider.HISTORY_CONTENT_TYPE_FILE:
                                text = "File";
                                break;
                            default:
                                text = message;
                                break;
                        }

                        NotificationLine line = new NotificationLine(nickName, text, largeIcon, openChatIntent, Arrays.asList(replyAction, readAction));
                        lines.add(line);
                    }

                    boolean privateNotifications = PreferenceHelper.isPrivateNotifications(context);

                    PendingIntent contentAction;
                    List<NotificationCompat.Action> actions;

                    String title;
                    StringBuilder text;
                    Bitmap image;
                    if (lines.size() > 1 || privateNotifications) {
                        PendingIntent openChatsIntent = createOpenChatsIntent(context, requestCode++);
                        PendingIntent readAllIntent = createReadAllIntent(context, requestCode);
                        NotificationCompat.Action chatsAction = new NotificationCompat.Action.Builder(
                                R.drawable.ic_chat, context.getString(R.string.dialogs), openChatsIntent)
                                .setAllowGeneratedReplies(true)
                                .build();
                        NotificationCompat.Action readAllAction = new NotificationCompat.Action.Builder(
                                R.drawable.ic_action_read, context.getString(R.string.mark_as_read_all), readAllIntent)
                                .setAllowGeneratedReplies(true)
                                .build();

                        title = context.getResources().getQuantityString(R.plurals.count_new_messages, unread, unread);
                        text = new StringBuilder();
                        for (NotificationLine line : lines) {
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(line.getTitle());
                        }
                        image = null;
                        contentAction = openChatsIntent;
                        actions = Arrays.asList(chatsAction, readAllAction);
                    } else {
                        NotificationLine line = lines.remove(0);
                        title = line.getTitle();
                        text = new StringBuilder(line.getText());
                        image = line.getImage();
                        contentAction = line.getContentAction();
                        actions = line.getActions();
                    }
                    boolean isAlert = false;
                    if (isAlarmRequired && isNotificationCompleted()) {
                        // Wzh-wzh!
                        onNotificationShown();
                        isAlert = true;
                        Logger.log("update notifications: wzh-wzh!");
                    }
                    final NotificationContent data = new NotificationContent(!privateNotifications, isAlert,
                            title, text.toString(), image, lines, contentAction, actions);
                    MainExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Notifier.showNotification(context, data);
                        }
                    });
                    // Update shown messages flag.
                    QueryHelper.updateShownMessagesFlag(contentResolver);
                } else {
                    Logger.log("HistoryObserver: No unread messages found");
                    onNotificationCancel();
                    MainExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Notifier.hideNotification(context);
                        }
                    });
                }
                if (onScreen > 0) {
                    Logger.log("HistoryObserver: Vibrate a little");
                    vibrate(80);
                    QueryHelper.updateOnScreenMessages(contentResolver);
                }
            } else {
                Logger.log("HistoryObserver: Non-shown messages not found");
            }
            Logger.log("History dispatching time: " + (System.currentTimeMillis() - time));
            // Call to update unread count.
            contentResolver.call(Settings.BUDDY_RESOLVER_URI, GlobalProvider.METHOD_UPDATE_UNREAD, null, null);
            settingsChanged = false;
        }

        private void onNotificationShown() {
            notificationCancelTime = System.currentTimeMillis() + Settings.NOTIFICATION_MIN_DELAY;
        }

        private void onNotificationCancel() {
            long notificationRemain = getNotificationRemain();
            if (notificationRemain > 0) {
                try {
                    // Take some time to read this message and notification to be shown
                    Thread.sleep(notificationRemain);
                } catch (InterruptedException ignored) {
                }
            }
        }

        private boolean isNotificationCompleted() {
            return getNotificationRemain() <= 0;
        }

        private long getNotificationRemain() {
            return notificationCancelTime - System.currentTimeMillis();
        }

        private void vibrate(long delay) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(delay);
        }
    }

    private static PendingIntent createOpenChatsIntent(Context context, int requestCode) {
        return PendingIntent.getActivity(context, requestCode,
                new Intent(context, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReadAllIntent(Context context, int requestCode) {
        return PendingIntent.getService(context, requestCode,
                new Intent(context, CoreService.class)
                        .putExtra(EXTRA_READ_MESSAGES, true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReadIntent(Context context, int buddyDbId, int requestCode) {
        return PendingIntent.getService(context, requestCode,
                new Intent(context, CoreService.class)
                        .putExtra(EXTRA_READ_MESSAGES, true)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createOpenChatIntent(Context context, int buddyDbId, int requestCode) {
        return PendingIntent.getActivity(context, requestCode,
                new Intent(context, ChatActivity.class)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent createReplyIntent(Context context, int buddyDbId, int requestCode) {
        return PendingIntent.getActivity(context, requestCode,
                new Intent(context, ChatActivity.class)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
