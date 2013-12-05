package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/4/13
 * Time: 8:37 PM
 */
public class AvatarRequest extends BitmapRequest<IcqAccountRoot> {

    private String buddyId;

    public AvatarRequest() {
    }

    public AvatarRequest(String buddyId, String url) {
        super(url);
        this.buddyId = buddyId;
    }

    @Override
    protected void onBitmapSaved(String hash) {
        Log.d(Settings.LOG_TAG, "Update destination buddy " + buddyId + " avatar hash to " + hash);
        try {
            QueryHelper.modifyAvatar(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId, hash);
            Log.d(Settings.LOG_TAG, "Avatar complex operations succeeded!");
        } catch (BuddyNotFoundException ignored) {
            Log.d(Settings.LOG_TAG, "Hm... Buddy became not found while avatar being downloaded...");
        }
    }
}
