package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import android.text.TextUtils;
import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Solkin on 25.07.2014.
 */
public class LargeAvatarRequest extends BitmapRequest<IcqAccountRoot> {

    private String buddyId;

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String BUDDY_ID = "buddy_id";

    public LargeAvatarRequest() {
    }

    public LargeAvatarRequest(String buddyId, String url) {
        super(url);
        this.buddyId = buddyId;
    }

    @Override
    protected void onBitmapSaved(String hash) {
        // Check for destination buddy is account.
        if(TextUtils.equals(buddyId, getAccountRoot().getUserId())) {
            Logger.log("Update profile " + getAccountRoot().getUserId() + " avatar hash to " + hash);
            getAccountRoot().setAvatarHash(hash);
            getAccountRoot().updateAccount();
        }
        try {
            QueryHelper.modifyBuddyAvatar(getAccountRoot().getContentResolver(),
                    getAccountRoot().getAccountDbId(), buddyId, hash);
        } catch (BuddyNotFoundException ignored) {
        }
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BUDDY_ID, buddyId);
        intent.putExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH, hash);
        getService().sendBroadcast(intent);
    }
}
