package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class BuddyInfoActivity extends AbstractInfoActivity {

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
            case R.id.buddy_info_share:
                startActivity(createShareIntent());
                return true;
            case R.id.buddy_info_copy:
                StringUtil.copyStringToClipboard(this, getShareString(), R.string.buddy_info_copied);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "BuddyInfoActivity onCreate");

        // Preparing for action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            bar.setTitle(R.string.buddy_info);
        }
    }

    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
    }

    private String getShareString() {
        StringBuilder shareString = new StringBuilder();
        // Checking and attaching first and last name.
        if (!TextUtils.isEmpty(getFirstName())) {
            shareString.append(getFirstName()).append(" ");
        }
        if (!TextUtils.isEmpty(getLastName())) {
            shareString.append(getLastName()).append(" ");
        }
        // Strong checking for buddy nick.
        if (!(TextUtils.isEmpty(getBuddyNick()) || TextUtils.equals(getBuddyNick(), getBuddyId()))) {
            if (TextUtils.isEmpty(shareString)) {
                shareString.append(getBuddyNick()).append(" ");
            } else {
                shareString.append("(").append(getBuddyNick()).append(") ");
            }
        }
        // Something appended? Appending dash.
        if (!TextUtils.isEmpty(shareString)) {
            shareString.append("- ");
        }
        shareString.append(getBuddyId());
        return shareString.toString();
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareString());
        return Intent.createChooser(shareIntent, getString(R.string.share_buddy_info_via));
    }
}
