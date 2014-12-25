package com.tomclaw.mandarin.main;

import android.app.Application;
import android.util.Log;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.Settings;
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
        long time = System.currentTimeMillis();
        StatusUtil.include(IcqAccountRoot.class.getName(), new IcqStatusCatalogue(this));
        BitmapCache.getInstance().init(this);
        super.onCreate();
        Log.d(Settings.LOG_TAG, "application start time: " + (System.currentTimeMillis() - time));
    }
}
