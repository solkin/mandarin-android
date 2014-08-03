package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created by solkin on 12/26/13.
 */
public abstract class AbstractInfoActivity extends ChiefActivity implements ChiefActivity.CoreServiceListener {

    private int accountDbId;
    private String accountType;
    private String buddyId;
    private String buddyNick;
    private String avatarHash;
    private int buddyStatus;
    private String firstName;
    private String lastName;

    private TextView buddyNickView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "AbstractInfoActivity onCreate");
        // We want to receive service state notifications.
        addCoreServiceListener(this);

        // Obtain and check basic info about interested buddy.
        Intent intent = getIntent();
        accountDbId = intent.getIntExtra(BuddyInfoRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        accountType = intent.getStringExtra(BuddyInfoRequest.ACCOUNT_TYPE);
        buddyId = intent.getStringExtra(BuddyInfoRequest.BUDDY_ID);
        buddyNick = intent.getStringExtra(BuddyInfoRequest.BUDDY_NICK);
        avatarHash = intent.getStringExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH);
        buddyStatus = intent.getIntExtra(BuddyInfoRequest.BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);

        // Check for required fields are correct.
        if (TextUtils.isEmpty(buddyId) || accountDbId == GlobalProvider.ROW_INVALID) {
            // Nothing we can do in this case. Only show toast and close activity.
            onBuddyInfoRequestError();
            finish();
            return;
        }

        // Obtain and check basic info about interested buddy.
        String buddyStatusTitle = intent.getStringExtra(BuddyInfoRequest.BUDDY_STATUS_TITLE);
        String buddyStatusMessage = intent.getStringExtra(BuddyInfoRequest.BUDDY_STATUS_MESSAGE);

        // Initialize info activity layout.
        setContentView(getLayout());

        TextView buddyIdView = (TextView) findViewById(R.id.buddy_id);
        buddyIdView.setText(getBuddyId());

        buddyNickView = (TextView) findViewById(R.id.buddy_nick);
        updateBuddyNick();

        if (!TextUtils.isEmpty(getAccountType()) && buddyStatusTitle != null) {
            int statusImageResource = StatusUtil.getStatusDrawable(getAccountType(), getBuddyStatus());

            if (getBuddyStatus() == StatusUtil.STATUS_OFFLINE
                    || TextUtils.equals(buddyStatusTitle, buddyStatusMessage)) {
                // Buddy status is offline now or status message is only status title.
                // No status message could be displayed.
                buddyStatusMessage = "";
            }
            final SpannableString statusString = new SpannableString(buddyStatusTitle + " " + buddyStatusMessage);
            statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, buddyStatusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Yeah, we have status info - so we might show status info.
            findViewById(R.id.info_status_title).setVisibility(View.VISIBLE);
            View statusContent = findViewById(R.id.info_status_content);
            statusContent.setVisibility(View.VISIBLE);
            statusContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StringUtil.copyStringToClipboard(AbstractInfoActivity.this, statusString.toString(), R.string.status_copied);
                }
            });

            ((ImageView) findViewById(R.id.status_icon)).setImageResource(statusImageResource);
            ((TextView) findViewById(R.id.status_text)).setText(statusString);
        }

        // Buddy avatar.
        ImageView contactBadge = (ImageView) findViewById(R.id.buddy_badge);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, getAvatarHash(), R.drawable.ic_default_avatar);
    }

    protected abstract int getLayout();

    public String getBuddyName() {
        return buddyNickView.getText().toString();
    }

    private void updateBuddyNick() {
        String nick = getBuddyName();
        if(TextUtils.isEmpty(nick) || TextUtils.equals(nick, buddyId)) {
            nick = getBuddyNick();
            if(TextUtils.isEmpty(nick)) {
                nick = StringUtil.appendIfNotEmpty(nick, getFirstName(), "");
                nick = StringUtil.appendIfNotEmpty(nick, getLastName(), " ");
            }
        }
        buddyNickView.setText(nick);
    }

    public abstract void onBuddyInfoRequestError();

    private void hideProgressBar() {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
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
            onBuddyInfoRequestError();
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Check for info present in this intent.
        boolean isInfoPresent = !intent.getBooleanExtra(BuddyInfoRequest.NO_INFO_CASE, false);
        int requestAccountDbId = intent.getIntExtra(BuddyInfoRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        String requestBuddyId = intent.getStringExtra(BuddyInfoRequest.BUDDY_ID);
        Log.d(Settings.LOG_TAG, "buddy id: " + requestBuddyId);
        // Checking for this is info we need.
        if (requestAccountDbId == accountDbId && TextUtils.equals(requestBuddyId, buddyId)) {
            // Hide the progress bar.
            hideProgressBar();
            if (isInfoPresent) {
                Bundle bundle = intent.getExtras();
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
                                setBuddyNick(value);
                            } else if (Integer.valueOf(key) == R.id.first_name) {
                                setFirstName(value);
                            } else if (Integer.valueOf(key) == R.id.last_name) {
                                setLastName(value);
                            }
                        }
                    }
                }
                updateBuddyNick();
            } else {
                Log.d(Settings.LOG_TAG, "No info case :(");
                onBuddyInfoRequestError();
            }
        } else {
            Log.d(Settings.LOG_TAG, "Wrong buddy info!");
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
