package com.tomclaw.mandarin.util;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.List;

/**
 * Created by ivsolkin on 06/07/2017.
 */

public class NotificationLine {

    private final @NonNull String title;
    private final @NonNull String text;
    private final @Nullable Bitmap image;
    private final @NonNull List<NotificationCompat.Action> actions;

    public NotificationLine(@NonNull String title, @NonNull String text, @Nullable Bitmap image,
                            @NonNull List<NotificationCompat.Action> actions) {
        this.title = title;
        this.text = text;
        this.image = image;
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

    @NonNull
    public List<NotificationCompat.Action> getActions() {
        return actions;
    }
}
