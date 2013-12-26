package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class BuddyInfoActivity extends AbstractInfoActivity {

    public static final String BUDDY_STATUS_TITLE = "buddy_status_title";
    public static final String BUDDY_STATUS_MESSAGE = "buddy_status_message";

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
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", getShareString()));
                Toast.makeText(this, R.string.buddy_info_copied, Toast.LENGTH_SHORT).show();
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
        String buddyStatusTitle = intent.getStringExtra(BUDDY_STATUS_TITLE);
        String buddyStatusMessage = intent.getStringExtra(BUDDY_STATUS_MESSAGE);

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
        buddyIdView.setText(getBuddyId());

        TextView buddyNickView = (TextView) findViewById(R.id.buddy_nick);
        buddyNickView.setText(getBuddyNick());

        if (!TextUtils.isEmpty(getAccountType()) && buddyStatusTitle != null) {
            // Status image.
            int statusImageResource = StatusUtil.getStatusDrawable(getAccountType(), getBuddyStatus());

            // Status text.
            if (getBuddyStatus() == StatusUtil.STATUS_OFFLINE
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
        ImageView contactBadge = (ImageView) findViewById(R.id.buddy_badge);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, getAvatarHash(), R.drawable.ic_default_avatar);
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
