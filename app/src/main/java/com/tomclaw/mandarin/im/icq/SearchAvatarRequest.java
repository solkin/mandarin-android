package com.tomclaw.mandarin.im.icq;

import android.content.Intent;

import com.tomclaw.mandarin.core.BitmapRequest;
import com.tomclaw.mandarin.core.CoreService;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Solkin on 25.07.2014.
 */
public class SearchAvatarRequest extends BitmapRequest<IcqAccountRoot> {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";

    private String buddyId;

    public SearchAvatarRequest() {
    }

    public SearchAvatarRequest(String buddyId, String url) {
        super(url);
        this.buddyId = buddyId;
    }

    @Override
    protected void onBitmapSaved(String hash) {
        Logger.log("Search avatar received for buddy " + buddyId + ", avatar hash is " + hash);
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BUDDY_ID, buddyId);
        intent.putExtra(BUDDY_AVATAR_HASH, hash);
        getService().sendBroadcast(intent);
    }
}
