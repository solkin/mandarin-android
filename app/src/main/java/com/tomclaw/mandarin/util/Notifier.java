package com.tomclaw.mandarin.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.Html;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.preferences.PreferenceHelper;

import java.util.List;

/**
 * Created by ivsolkin on 06/07/2017.
 */
@SuppressWarnings("SameParameterValue")
public class Notifier {

    private static final int NOTIFICATION_ID = 0x05;
    private static final String NOTIFICATION_CHANNEL_ID = "messages";
    private static final String GROUP_KEY = "mandarin_notification";

    public static void init(Context context) {
        prepareChannel(context, NOTIFICATION_CHANNEL_ID, R.string.incoming_messages);
    }

    public static void showNotification(Context context, NotificationData data) {
        @DrawableRes int smallImage = R.drawable.ic_notification;
        @ColorInt int color = context.getResources().getColor(R.color.accent_color);
        if (data.isMultiline()) {
            showMultiNotification(context, NOTIFICATION_CHANNEL_ID, NOTIFICATION_ID, data.isExtended(), data.isAlert(), data.getTitle(),
                    data.getText(), data.getLines(), color, smallImage, data.getContentAction(),
                    data.getActions());
        } else {
            showSingleNotification(context, NOTIFICATION_CHANNEL_ID, NOTIFICATION_ID, data.isExtended(), data.isAlert(), data.getTitle(),
                    data.getText(), color, smallImage, data.getImage(), data.getContentAction(),
                    data.getActions());
        }
    }

    public static void hideNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
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

        if (isExtended && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

            int index = notificationId;
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
                notificationManager.notify(++index, notification.build());
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

    private static void prepareChannel(Context context, String channelId, @StringRes int channelName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    channelId,
                    context.getString(channelName),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(context.getResources().getColor(R.color.accent_color));
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private static void setNotificationAlert(Context context, NotificationCompat.Builder notification) {
        if (PreferenceHelper.isSound(context)) {
            notification.setSound(PreferenceHelper.getNotificationUri(context));
        }
        if (PreferenceHelper.isVibrate(context)) {
            notification.setVibrate(new long[]{0, 750});
        }
        if (PreferenceHelper.isLights(context)) {
            notification.setLights(Settings.LED_COLOR_RGB,
                    Settings.LED_BLINK_DELAY, Settings.LED_BLINK_DELAY);
        }
    }

}
