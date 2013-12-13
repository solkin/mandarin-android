package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class BuddyInfoActivity extends ChiefActivity {

    public static final String BUDDY_ID = "buddy_id";
    public static final String BUDDY_NICK = "buddy_nick";
    public static final String BUDDY_AVATAR_HASH = "buddy_avatar_hash";
    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String NO_INFO_CASE = "no_info_case";

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

        Intent intent = getIntent();
        accountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        buddyId = intent.getStringExtra(BUDDY_ID);
        String buddyNick = intent.getStringExtra(BUDDY_NICK);
        String avatarHash = intent.getStringExtra(BUDDY_AVATAR_HASH);
        if(TextUtils.isEmpty(buddyId) || accountDbId == GlobalProvider.ROW_INVALID) {
            Toast.makeText(this, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.buddy_info);

        // Initialize accounts list
        setContentView(R.layout.buddy_info_activity);

        TextView buddyIdView = (TextView) findViewById(R.id.buddy_id);
        buddyIdView.setText(buddyId);

        TextView buddyNickView = (TextView) findViewById(R.id.buddy_nick);
        buddyNickView.setText(buddyNick);

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
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
        boolean isInfoPresent = !intent.getBooleanExtra(NO_INFO_CASE, false);
        if(isInfoPresent) {
            String buddyId = intent.getStringExtra(BUDDY_ID);
            Log.d(Settings.LOG_TAG, "buddy id: " + buddyId);
        } else {
            Log.d(Settings.LOG_TAG, "No info case :(");
        }
    }
}
