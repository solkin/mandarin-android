package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/4/13
 * Time: 8:37 PM
 */
public class BuddyAvatarRequest extends BitmapRequest<IcqAccountRoot> {

    private String buddyId;

    public BuddyAvatarRequest() {
    }

    public BuddyAvatarRequest(String buddyId, String url) {
        super(url);
        this.buddyId = buddyId;
    }

    @Override
    protected void onBitmapSaved(String hash) {
        Logger.log("Update destination buddy " + buddyId + " avatar hash to " + hash);
        try {
            QueryHelper.modifyBuddyAvatar(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId, hash);
            Logger.log("Avatar complex operations succeeded!");
        } catch (BuddyNotFoundException ignored) {
            Logger.log("Hm... Buddy became not found while avatar being downloaded...");
        }
    }
}
