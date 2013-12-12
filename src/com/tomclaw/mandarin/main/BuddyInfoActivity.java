package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;
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

    private int buddyDbId;
    private int accountDbId;
    private String buddyId;
    private String buddyNick;
    private String avatarHash;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "BuddyInfoActivity onCreate");

        Intent intent = getIntent();
        buddyDbId = intent.getIntExtra("BUDDY_DB_ID", -1);

        // Obtain basic buddy info.
        // TODO: out this method.
        Cursor cursor = getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Cursor may have more than only one entry.
        // TODO: check for at least one buddy present.
        if (cursor.moveToFirst()) {
            accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
            buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
            buddyNick = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK));
            avatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH));
        }
        cursor.close();

        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.accounts);

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
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onCoreServiceDown() {

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
        String buddyId = intent.getStringExtra("BUDDY_ID");
        Log.d(Settings.LOG_TAG, "buddy id: " + buddyId);
    }
}
