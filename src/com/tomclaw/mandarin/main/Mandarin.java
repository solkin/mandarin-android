package com.tomclaw.mandarin.main;

import android.app.Application;
import com.tomclaw.mandarin.core.ExceptionHandler;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.11.13
 * Time: 14:08
 */
public class Mandarin extends Application {

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(this));
        super.onCreate();
    }
}
