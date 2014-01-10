package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created by solkin on 12/21/13.
 */
public class AccountAvatarRequest extends BitmapRequest<IcqAccountRoot> {

    public AccountAvatarRequest() {
    }

    public AccountAvatarRequest(String url) {
        super(url);
    }

    @Override
    protected void onBitmapSaved(String hash) {
        Log.d(Settings.LOG_TAG, "Update destination profile " + getAccountRoot().getUserId() + " avatar hash to " + hash);
        getAccountRoot().setAvatarHash(hash);
        getAccountRoot().updateAccount();
        Log.d(Settings.LOG_TAG, "Avatar complex operations succeeded!");
    }
}
