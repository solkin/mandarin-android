package com.tomclaw.mandarin.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.Settings;

import java.util.List;

import static com.tomclaw.mandarin.util.StringUtil.sha1;

/**
 * Created by ivsolkin on 06/07/2017.
 */
@SuppressWarnings("SameParameterValue")
public class Notifier {

    private static final int NOTIFICATION_ID = 0x05;
    private static final String GROUP_KEY = "mandarin_notification";

    public static void showNotification(Context context, NotificationContent data) {
        @DrawableRes int smallImage = R.drawable.ic_notification;
        @ColorInt int color = context.getResources().getColor(R.color.accent_color);
        String channelId = getChannelId(context);
        if (data.isMultiline()) {
            showMultiNotification(context, channelId, NOTIFICATION_ID, data.isExtended(), data.isAlert(), data.getTitle(),
                    data.getText(), data.getLines(), color, smallImage, data.getContentAction(),
                    data.getActions());
        } else {
            showSingleNotification(context, channelId, NOTIFICATION_ID, data.isExtended(), data.isAlert(), data.getTitle(),
                    data.getText(), color, smallImage, data.getImage(), data.getContentAction(),
                    data.getActions());
        }
    }

    public static void hideNotification(Context context) {
        hideNotification(context, NOTIFICATION_ID);
    }

    public static void hideNotification(Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
    }

    private static void showSingleNotification(Context context, String channelId, int notificationId, boolean isExtended,
                                               boolean isAlert, @NonNull String title, @NonNull String text,
                                               @ColorInt int color, @DrawableRes int smallImage, @Nullable Bitmap image,
                                               @Nullable PendingIntent contentAction,
                                               @NonNull List<NotificationCompat.Action> actions) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(color)
                .setSmallIcon(smallImage)
                .setLargeIcon(image)
                .setShowWhen(true);
        if (contentAction != null) {
            notification.setContentIntent(contentAction);
        }
        if (isExtended) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(text);

            notification.setStyle(style);
        }
        for (NotificationCompat.Action action : actions) {
            notification.addAction(action);
        }

        if (isAlert) {
            setNotificationAlert(context, notification);
        }

        notificationManager.notify(notificationId, notification.build());
    }

    private static void showMultiNotification(Context context, String channelId, int notificationId, boolean isExtended,
                                              boolean isAlert, @NonNull String title, @NonNull String text,
                                              @NonNull List<NotificationLine> lines, @ColorInt int color,
                                              @DrawableRes int smallImage,
                                              @Nullable PendingIntent contentAction,
                                              @NonNull List<NotificationCompat.Action> actions) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (isExtended && isGroupedNotifications()) {
            NotificationCompat.Builder group = new NotificationCompat.Builder(context, channelId)
                    .setGroup(GROUP_KEY)
                    .setColor(color)
                    .setSmallIcon(smallImage)
                    .setGroupSummary(true)
                    .setContentIntent(contentAction);

            if (isAlert) {
                setNotificationAlert(context, group);
            }

            notificationManager.notify(notificationId, group.build());

            for (NotificationLine line : lines) {
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(line.getTitle())
                        .bigText(line.getText());
                NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
                        .setStyle(style)
                        .setContentTitle(line.getTitle())
                        .setContentText(line.getText())
                        .setColor(color)
                        .setSmallIcon(smallImage)
                        .setLargeIcon(line.getImage())
                        .setGroup(GROUP_KEY);
                if (contentAction != null) {
                    notification.setContentIntent(line.getContentAction());
                }
                for (NotificationCompat.Action action : line.getActions()) {
                    notification.addAction(action);
                }
                notificationManager.notify(line.getNotificationId(), notification.build());
            }
        } else {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setColor(color)
                    .setSmallIcon(smallImage)
                    .setContentIntent(contentAction);
            if (isExtended) {
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                style.setBigContentTitle(title);
                for (NotificationLine line : lines) {
                    style.addLine(Html.fromHtml("<b>" + line.getTitle() + "</b> " + line.getText()));
                }
                notification.setStyle(style);
            }
            for (NotificationCompat.Action action : actions) {
                notification.addAction(action);
            }

            if (isAlert) {
                setNotificationAlert(context, notification);
            }

            notificationManager.notify(notificationId, notification.build());
        }
    }

    private static String getNotificationPrefsHash(Context context) {
        String data = "";
        if (PreferenceHelper.isSound(context)) {
            Uri uri = PreferenceHelper.getNotificationUri(context);
            data += uri.toString();
        }
        if (PreferenceHelper.isVibrate(context)) {
            data += "vibrate";
        }
        if (PreferenceHelper.isLights(context)) {
            data += "lights";
        }
        return sha1(data);
    }

    private static String getChannelId(Context context) {
        String prefsHash = getNotificationPrefsHash(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(prefsHash) != null) {
                return prefsHash;
            }
            NotificationChannel notificationChannel = new NotificationChannel(
                    prefsHash,
                    context.getString(R.string.incoming_messages),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            if (PreferenceHelper.isSound(context)) {
                Uri uri = PreferenceHelper.getNotificationUri(context);
                AudioAttributes attrs = RingtoneManager.getRingtone(context, uri).getAudioAttributes();
                notificationChannel.setSound(uri, attrs);
            } else {
                notificationChannel.setSound(null, null);
            }
            if (PreferenceHelper.isVibrate(context)) {
                notificationChannel.enableVibration(true);
                notificationChannel.setVibrationPattern(new long[]{0, 100, 0, 100});
            } else {
                notificationChannel.enableVibration(false);
            }
            if (PreferenceHelper.isLights(context)) {
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Settings.LED_COLOR_RGB);
            } else {
                notificationChannel.enableLights(false);
            }
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return prefsHash;
    }

    private static void setNotificationAlert(Context context, NotificationCompat.Builder notification) {
        if (PreferenceHelper.isSound(context)) {
            notification.setSound(PreferenceHelper.getNotificationUri(context));
        }
        if (PreferenceHelper.isVibrate(context)) {
            notification.setVibrate(new long[]{0, 100, 0, 100});
        }
        if (PreferenceHelper.isLights(context)) {
            notification.setLights(Settings.LED_COLOR_RGB,
                    Settings.LED_BLINK_DELAY, Settings.LED_BLINK_DELAY);
        }
    }

    public static boolean isGroupedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

}