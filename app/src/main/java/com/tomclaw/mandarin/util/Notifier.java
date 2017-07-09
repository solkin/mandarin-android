package com.tomclaw.mandarin.util;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;

import com.tomclaw.mandarin.R;

import java.util.List;

/**
 * Created by ivsolkin on 06/07/2017.
 */
public class Notifier {

    private static final int NOTIFICATION_ID = 0x05;
    private static final String GROUP_KEY = "mandarin_notification";

    public static void showNotification(Context context, NotificationData data) {
        @DrawableRes int smallImage = R.drawable.ic_notification;
        @ColorInt int color = context.getResources().getColor(R.color.accent_color);
        if (data.isMultiline()) {
            showMultiNotification(context, NOTIFICATION_ID, data.isExtended(), data.getTitle(),
                    data.getText(), data.getLines(), color, smallImage, data.getContentAction(),
                    data.getActions());
        } else {
            showSingleNotification(context, NOTIFICATION_ID, data.isExtended(), data.getTitle(),
                    data.getText(), color, smallImage, data.getImage(), data.getContentAction(),
                    data.getActions());
        }
    }

    public static void hideNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private static void showSingleNotification(Context context, int notificationId, boolean isExtended,
                                               @NonNull String title, @NonNull String text,
                                               @ColorInt int color, @DrawableRes int smallImage, @Nullable Bitmap image,
                                               @Nullable PendingIntent contentAction,
                                               @NonNull List<NotificationCompat.Action> actions) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
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

        notificationManager.notify(notificationId, notification.build());
    }

    private static void showMultiNotification(Context context, int notificationId, boolean isExtended,
                                              @NonNull String title, @NonNull String text,
                                              @NonNull List<NotificationLine> lines, @ColorInt int color,
                                              @DrawableRes int smallImage,
                                              @Nullable PendingIntent contentAction,
                                              @NonNull List<NotificationCompat.Action> actions) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (isExtended && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationCompat.Builder group = new NotificationCompat.Builder(context)
                    .setGroup(GROUP_KEY)
                    .setColor(color)
                    .setSmallIcon(smallImage)
                    .setGroupSummary(true)
                    .setContentIntent(contentAction);

            notificationManager.notify(notificationId, group.build());

            int index = notificationId;
            for (NotificationLine line : lines) {
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(line.getTitle())
                        .bigText(line.getText());
                NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
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
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
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

            notificationManager.notify(notificationId, notification.build());
        }
    }

}
