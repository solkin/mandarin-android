package com.tomclaw.mandarin.util;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.List;

/**
 * Created by ivsolkin on 06/07/2017.
 */
public class NotificationData {

    private final boolean isExtended;
    private final @NonNull String title;
    private final @NonNull String text;
    private final @Nullable Bitmap image;
    private final @NonNull List<NotificationLine> lines;
    private final @Nullable PendingIntent contentAction;
    private final @NonNull List<NotificationCompat.Action> actions;

    public NotificationData(boolean isExtended, @NonNull String title, @NonNull String text,
                            @Nullable Bitmap image, @NonNull List<NotificationLine> lines,
                            @Nullable PendingIntent contentAction,
                            @NonNull List<NotificationCompat.Action> actions) {
        this.isExtended = isExtended;
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
