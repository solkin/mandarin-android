package com.tomclaw.mandarin.util;

import android.app.PendingIntent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * Created by ivsolkin on 06/07/2017.
 */
@SuppressWarnings("WeakerAccess")
public class NotificationData {

    private final boolean isExtended;
    private final boolean isAlert;
    @NonNull private final String title;
    @NonNull private final String text;
    @Nullable private final Bitmap image;
    @NonNull private final List<NotificationLine> lines;
    @Nullable private final PendingIntent contentAction;
    @NonNull private final List<NotificationCompat.Action> actions;

    public NotificationData(
            boolean isExtended, boolean isAlert, @NonNull String title,
            @NonNull String text, @Nullable Bitmap image,
            @NonNull List<NotificationLine> lines,
            @Nullable PendingIntent contentAction,
            @NonNull List<NotificationCompat.Action> actions
    ) {
        this.isExtended = isExtended;
        this.isAlert = isAlert;
        this.title = title;
        this.text = text;
        this.image = image;
        this.lines = lines;
        this.contentAction = contentAction;
        this.actions = actions;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public boolean isAlert() {
        return isAlert;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public boolean isMultiline() {
        return !lines.isEmpty();
    }

    @NonNull
    public List<NotificationLine> getLines() {
        return lines;
    }

    @Nullable
    public Bitmap getImage() {
        return image;
    }

    @Nullable
    public PendingIntent getContentAction() {
        return contentAction;
    }

    @NonNull
    public List<NotificationCompat.Action> getActions() {
        return actions;
    }
}
