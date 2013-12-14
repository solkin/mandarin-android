package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class BuddyInfoActivity extends ChiefActivity {

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_NICK = "buddy_nick";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";
    public static final String BUDDY_STATUS = "buddy_status";
    public static final String BUDDY_STATUS_TITLE = "buddy_status_title";
    public static final String BUDDY_STATUS_MESSAGE = "buddy_status_message";

    public static final String NO_INFO_CASE = "no_info_case";

    private static final String NUMERIC_REGEXP = "^[0-9]*$";

    private int accountDbId;
    private String buddyId;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.buddy_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "BuddyInfoActivity onCreate");

        // Obtain and check basic info about interested buddy.
        Intent intent = getIntent();
        accountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        String accountType = intent.getStringExtra(ACCOUNT_TYPE);
        buddyId = intent.getStringExtra(BUDDY_ID);
        String buddyNick = intent.getStringExtra(BUDDY_NICK);
        String avatarHash = intent.getStringExtra(BUDDY_AVATAR_HASH);
        int buddyStatus = intent.getIntExtra(BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
        String buddyStatusTitle = intent.getStringExtra(BUDDY_STATUS_TITLE);
        String buddyStatusMessage = intent.getStringExtra(BUDDY_STATUS_MESSAGE);
        // Check for required fields are correct.
        if (TextUtils.isEmpty(buddyId) || accountDbId == GlobalProvider.ROW_INVALID) {
            //Nothing we can do in this case. Only show toast and close activity.
            Toast.makeText(this, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Preparing for action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            bar.setTitle(R.string.buddy_info);
        }

        // Initialize info activity layout.
        setContentView(R.layout.buddy_info_activity);

        TextView buddyIdView = (TextView) findViewById(R.id.buddy_id);
        buddyIdView.setText(buddyId);

        TextView buddyNickView = (TextView) findViewById(R.id.buddy_nick);
        buddyNickView.setText(buddyNick);

        if (!TextUtils.isEmpty(accountType) && buddyStatusTitle != null) {
            // Status image.
            int statusImageResource = StatusUtil.getStatusDrawable(accountType, buddyStatus);

            // Status text.
            if (buddyStatus == StatusUtil.STATUS_OFFLINE
                    || TextUtils.equals(buddyStatusTitle, buddyStatusMessage)) {
                // Buddy status is offline now or status message is only status title.
                // No status message could be displayed.
                buddyStatusMessage = "";
            }
            SpannableString statusString = new SpannableString(buddyStatusTitle + " " + buddyStatusMessage);
            statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, buddyStatusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Yeah, we have status info - so we might show status info.
            findViewById(R.id.info_status_title).setVisibility(View.VISIBLE);
            findViewById(R.id.info_status_content).setVisibility(View.VISIBLE);

            ((ImageView) findViewById(R.id.status_icon)).setImageResource(statusImageResource);
            ((TextView) findViewById(R.id.status_text)).setText(statusString);
        }

        // Buddy avatar.
        QuickContactBadge contactBadge = (QuickContactBadge) findViewById(R.id.buddy_badge);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar);
    }

    @Override
    public void onCoreServiceReady() {
        try {
            String appSession = getServiceInteraction().getAppSession();
            ContentResolver contentResolver = getContentResolver();
            // Sending protocol buddy info request.
            RequestHelper.requestBuddyInfo(contentResolver, appSession, accountDbId, buddyId);
        } catch (Throwable ex) {
            Log.d(Settings.LOG_TAG, "Unable to publish buddy info request due to exception.", ex);
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Hide the progress bar.
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
        // Check for info present in this intent.
        boolean isInfoPresent = !intent.getBooleanExtra(NO_INFO_CASE, false);
        Bundle bundle = intent.getExtras();
        if (isInfoPresent && bundle != null) {
            String buddyId = intent.getStringExtra(BUDDY_ID);
            Log.d(Settings.LOG_TAG, "buddy id: " + buddyId);

            int requestAccountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
            String requestBuddyId = intent.getStringExtra(BUDDY_ID);

            // Checking for this is info we need.
            if (requestAccountDbId == accountDbId && TextUtils.equals(requestBuddyId, buddyId)) {
                findViewById(R.id.info_base_title).setVisibility(View.VISIBLE);
                findViewById(R.id.info_extended_title).setVisibility(View.VISIBLE);

                // Iterate by info keys.
                for (String key : bundle.keySet()) {
                    if (key.matches(NUMERIC_REGEXP)) {
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
                        }
                    }
                }
            }
        } else {
            Log.d(Settings.LOG_TAG, "No info case :(");
        }
    }
}
