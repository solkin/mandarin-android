package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnketaActivity extends ChiefActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    //To change body of overridden methods use File | Settings | File Templates.
        Log.d(LOG_TAG, "AnketaActivity onCreate");
    }

    @Override
    public void onCoreServiceReady() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCoreServiceDown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
