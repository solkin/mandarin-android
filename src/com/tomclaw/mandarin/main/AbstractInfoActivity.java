package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created by solkin on 12/26/13.
 */
public abstract class AbstractInfoActivity extends ChiefActivity {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_NICK = "buddy_nick";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";
    public static final String BUDDY_STATUS = "buddy_status";

    public static final String NO_INFO_CASE = "no_info_case";

    private int accountDbId;
    private String accountType;
    private String buddyId;
    private String buddyNick;
    private String avatarHash;
    private int buddyStatus;
    private String firstName;
    private String lastName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "BuddyInfoActivity onCreate");

        // Obtain and check basic info about interested buddy.
        Intent intent = getIntent();
        accountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        accountType = intent.getStringExtra(ACCOUNT_TYPE);
        buddyId = intent.getStringExtra(BUDDY_ID);
        buddyNick = intent.getStringExtra(BUDDY_NICK);
        avatarHash = intent.getStringExtra(BUDDY_AVATAR_HASH);
        buddyStatus = intent.getIntExtra(BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
        // Check for required fields are correct.
        if (TextUtils.isEmpty(buddyId) || accountDbId == GlobalProvider.ROW_INVALID) {
            // Nothing we can do in this case. Only show toast and close activity.
            onBuddyInfoRequestError();
            finish();
            return;
        }
    }

    public abstract void onBuddyInfoRequestError();

    @Override
    public void onCoreServiceReady() {
        try {
            String appSession = getServiceInteraction().getAppSession();
            ContentResolver contentResolver = getContentResolver();
            // Sending protocol buddy info request.
            RequestHelper.requestBuddyInfo(contentResolver, appSession, accountDbId, buddyId);
        } catch (Throwable ex) {
            Log.d(Settings.LOG_TAG, "Unable to publish buddy info request due to exception.", ex);
            onBuddyInfoRequestError();
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Check for info present in this intent.
        boolean isInfoPresent = !intent.getBooleanExtra(NO_INFO_CASE, false);
        Bundle bundle = intent.getExtras();
        if (isInfoPresent && bundle != null) {
            int requestAccountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
            String requestBuddyId = intent.getStringExtra(BUDDY_ID);
            Log.d(Settings.LOG_TAG, "buddy id: " + requestBuddyId);
            // Checking for this is info we need.
            if (requestAccountDbId == accountDbId && TextUtils.equals(requestBuddyId, buddyId)) {
                // Hide the progress bar.
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                // Show info blocks.
                findViewById(R.id.info_base_title).setVisibility(View.VISIBLE);
                findViewById(R.id.info_extended_title).setVisibility(View.VISIBLE);
                // Iterate by info keys.
                for (String key : bundle.keySet()) {
                    if (StringUtil.isNumeric(key)) {
                        String[] extra = intent.getStringArrayExtra(key);
                        // Strange, but... Let's check extra is not empty.
                        if (extra != null && extra.length >= 1) {
                            String title = getString(Integer.parseInt(extra[0]));
                            String value = extra[1];
                            // Prepare buddy info item.
                            View buddyInfoItem = findViewById(Integer.valueOf(key));
                            if (buddyInfoItem != null) {
                                ((TextView) buddyInfoItem.findViewById(R.id.info_title)).setText(title);
                                ((TextView) buddyInfoItem.findViewById(R.id.info_value)).setText(value);
                                buddyInfoItem.setVisibility(View.VISIBLE);
                            }
                            // Correct user-defined values for sharing.
                            if (Integer.valueOf(key) == R.id.friendly_name) {
                                buddyNick = value;
                            } else if (Integer.valueOf(key) == R.id.first_name) {
                                firstName = value;
                            } else if (Integer.valueOf(key) == R.id.last_name) {
                                lastName = value;
                            }
                        }
                    }
                }
            } else {
                Log.d(Settings.LOG_TAG, "Wrong buddy info!");
            }
        } else {
            Log.d(Settings.LOG_TAG, "No info case :(");
            onBuddyInfoRequestError();
        }
    }

    public int getAccountDbId() {
        return accountDbId;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getBuddyId() {
        return buddyId;
    }

    public String getBuddyNick() {
        return buddyNick;
    }

    public void setBuddyNick(String buddyNick) {
        this.buddyNick = buddyNick;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public int getBuddyStatus() {
        return buddyStatus;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
