package com.tomclaw.mandarin.im.icq;

import android.content.Intent;
import com.tomclaw.mandarin.core.CoreService;

import java.util.List;

/**
 * Created by Solkin on 20.12.2015.
 */
public class BuddyIgnoreStateRequest extends GetPermitDenyRequest {

    private String buddyId;

    public BuddyIgnoreStateRequest() {
    }

    public BuddyIgnoreStateRequest(String buddyId) {
        this.buddyId = buddyId;
    }

    @Override
    protected void onPermitDenyInfoReceived(String pdMode, List<String> allows, List<String> blocks, List<String> ignores) {
        Intent intent = new Intent(CoreService.ACTION_CORE_SERVICE);
        intent.putExtra(CoreService.EXTRA_STAFF_PARAM, false);
        intent.putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, getAccountRoot().getAccountDbId());
        intent.putExtra(BuddyInfoRequest.BUDDY_ID, buddyId);
        intent.putExtra(BuddyInfoRequest.BUDDY_IGNORED, ignores.contains(buddyId) ? 1 : 0);
        getService().sendBroadcast(intent);
    }
}
