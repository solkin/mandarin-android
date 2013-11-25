package com.tomclaw.mandarin.main;

import android.app.Application;
import com.tomclaw.mandarin.core.ExceptionHandler;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.icq.IcqStatusCatalogue;

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
        StatusUtil.include(IcqAccountRoot.class.getName(), new IcqStatusCatalogue(this));
        super.onCreate();
    }
}
