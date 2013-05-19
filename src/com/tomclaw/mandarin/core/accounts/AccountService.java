package com.tomclaw.mandarin.core.accounts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/18/13
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new AccountAuthenticator(this).getIBinder();
    }
}
