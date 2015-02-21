package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.util.Logger;

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
        Logger.log("Update destination profile " + getAccountRoot().getUserId() + " avatar hash to " + hash);
        getAccountRoot().setAvatarHash(hash);
        getAccountRoot().updateAccount();
        Logger.log("Avatar complex operations succeeded!");
    }
}
