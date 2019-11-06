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
public class NotificationLine {

    private final @NonNull String title;
    private final @NonNull String text;
    private final @Nullable Bitmap image;
    private final @Nullable PendingIntent contentAction;
    private final @NonNull List<NotificationCompat.Action> actions;

    public NotificationLine(@NonNull String title, @NonNull String text, @Nullable Bitmap image,
                            @Nullable PendingIntent contentAction,
                            @NonNull List<NotificationCompat.Action> actions) {
        this.title = title;
        this.text = text;
        this.image = image;
        this.contentAction = contentAction;
        this.actions = actions;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getText() {
        return text;
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
