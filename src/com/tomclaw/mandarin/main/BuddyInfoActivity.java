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
        String shareString = "";
        // Checking and attaching first and last name.
        shareString = StringUtil.appendIfNotEmpty(shareString, getFirstName(), "");
        shareString = StringUtil.appendIfNotEmpty(shareString, getLastName(), " ");
        // Strong checking for buddy nick.
        if (!(TextUtils.isEmpty(getBuddyNick()) || TextUtils.equals(getBuddyNick(), getBuddyId())) &&
                !TextUtils.equals(shareString, getBuddyNick())) {
            String buddyNick = getBuddyNick();
            if (!TextUtils.isEmpty(shareString)) {
                buddyNick = "(" + buddyNick + ")";
            }
            shareString = StringUtil.appendIfNotEmpty(shareString, buddyNick, " ");
        }
        // Appending user id.
        shareString = StringUtil.appendIfNotEmpty(shareString, getBuddyId(), " - ");
        return shareString;
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareString());
        return Intent.createChooser(shareIntent, getString(R.string.share_buddy_info_via));
    }
}
