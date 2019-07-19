package com.tomclaw.helpers;

import android.os.SystemClock;

/**
 * Created by ivsolkin on 27.11.16.
 */
public class Timer {

    private long startPoint;

    public Timer start() {
        startPoint = SystemClock.elapsedRealtime();
        return this;
    }

    public long stop() {
        return SystemClock.elapsedRealtime() - startPoint;
    }
}
