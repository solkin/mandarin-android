package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusActitvity extends ChiefActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    //To change body of overridden methods use File | Settings | File Templates.
        Log.d(Settings.LOG_TAG, "StatusActivity onCreate");
    }

    @Override
    public void onCoreServiceReady() {

    }

    @Override
    public void onCoreServiceDown() {

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }
}
