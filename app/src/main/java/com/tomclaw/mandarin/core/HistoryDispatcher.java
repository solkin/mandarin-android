package com.tomclaw.mandarin.core;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;

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

import static com.tomclaw.mandarin.util.Notifier.isGroupedNotifications;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 28.09.13
 * Time: 15:40
 */
class HistoryDispatcher {

    private static final long HISTORY_DISPATCH_DELAY = 750;
    private static final int NOTIFICATION_ID_OFFSET = 100000;

    static String EXTRA_READ_MESSAGES = "read_messages";

    private Context context;
    private ContentResolver contentResolver;
    private ContentObserver historyObserver;
    private volatile long notificationCancelTime = 0;

    private final int largeIconSize;

    private boolean privateNotifications, settingsChanged;

    private List<NotificationLine> activeNotifications = emptyList();

    HistoryDispatcher(Context context) {
        // Variables.
        this.context = context;
        contentResolver = context.getContentResolver();
        // Creating observers.
        historyObserver = new HistoryObserver();
        largeIconSize = BitmapCache.convertDpToPixel(64, context);
    }

    void startObservation() {
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
        HistoryObserver() {
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
            if (bundle == null) {
                return;
            }
            int unshown = bundle.getInt(GlobalProvider.KEY_UNSHOWN);
            int justShown = bundle.getInt(GlobalProvider.KEY_JUST_SHOWN);
            int onScreen = bundle.getInt(GlobalProvider.KEY_ON_SCREEN);
            // Checking for non-shown messages exist.
            // If yes - we must update notification with all unread messages. If no - nothing to do now.
            if (unshown > 0 || justShown > 0 || onScreen > 0 || settingsChanged) {
                bundle = contentResolver.call(Settings.HISTORY_RESOLVER_URI, GlobalProvider.METHOD_GET_UNREAD, null, null);
                if (bundle == null) {
                    return;
                }
                final ArrayList<NotificationData> unreadList =
                        (ArrayList<NotificationData>) bundle.getSerializable(GlobalProvider.KEY_NOTIFICATION_DATA);
                boolean isAlarmRequired = (unshown > 0);
                // Checking for unread messages exist. If no, we must cancel notification.
                if (unreadList != null && !unreadList.isEmpty()) {
                    // Building variables.
                    int unread = 0;
                    boolean isSilentCancelNotification = justShown > 0 && unshown == 0 && isGroupedNotifications();
                    if (isSilentCancelNotification) {
                        MainExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                List<NotificationLine> lines = new ArrayList<>();
                                for (NotificationLine line : activeNotifications) {
                                    boolean isPresent = false;
                                    for (NotificationData data : unreadList) {
                                        int notificationId = data.getBuddyDbId() + NOTIFICATION_ID_OFFSET;
                                        if (notificationId == line.getNotificationId()) {
                                            isPresent = true;
                                        }
                                    }
                                    if (isPresent) {
                                        lines.add(line);
                                    } else {
                                        Notifier.hideNotification(context, line.getNotificationId());
                                    }
                                }
                                activeNotifications = lines;
                            }
                        });
                    } else {
                        // Last-message variables.
                        Bitmap largeIcon = null;
                        int requestCode = 1;
                        List<NotificationLine> lines = new ArrayList<>();
                        for (NotificationData data : unreadList) {
                            // Obtaining and collecting message-specific data.
                            unread += data.getUnreadCount();
                            String message = data.getMessageText();
                            final int buddyDbId = data.getBuddyDbId();
                            String nickName = data.getBuddyNick();
                            String avatarHash = data.getBuddyAvatarHash();
                            int contentType = data.getContentType();
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

                            int notificationId = buddyDbId + NOTIFICATION_ID_OFFSET;
                            NotificationLine line = new NotificationLine(
                                    notificationId,
                                    nickName,
                                    text,
                                    largeIcon,
                                    openChatIntent,
                                    Arrays.asList(replyAction, readAction)
                            );
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
                                activeNotifications = unmodifiableList(data.getLines());
                                Notifier.showNotification(context, data);
                            }
                        });
                    }
                    // Update shown messages flag.
                    QueryHelper.updateShownMessagesFlag(contentResolver);
                } else {
                    Logger.log("HistoryObserver: No unread messages found");
                    onNotificationCancel();
                    MainExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            activeNotifications = emptyList();
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

        @SuppressWarnings("SameParameterValue")
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
