package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.views.ContactImage;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Created by solkin on 12/26/13.
 */
public abstract class AbstractInfoActivity extends ChiefActivity
        implements ChiefActivity.CoreServiceListener, NfcAdapter.CreateNdefMessageCallback {

    private int accountDbId;
    private String accountType;
    private String buddyId;
    private String buddyNick;
    private String avatarHash;
    private int buddyStatus;
    private String firstName;
    private String lastName;
    private String aboutMe;

    private TextView buddyNickView;

    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("AbstractInfoActivity onCreate");
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
            // findViewById(R.id.info_status_title).setVisibility(View.VISIBLE);
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
        ContactImage contactBadge = (ContactImage) findViewById(R.id.buddy_image);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, getAvatarHash(), getDefaultAvatar(), false);
        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View container = findViewById(R.id.buddy_image_container);
                int targetHeight;
                if (container.getWidth() != container.getHeight()) {
                    targetHeight = container.getWidth();
                } else {
                    targetHeight = (int) getResources().getDimension(R.dimen.buddy_info_avatar_height);
                }
                ResizeAnimation resizeAnimation = new ResizeAnimation(container, targetHeight);
                resizeAnimation.setInterpolator(new OvershootInterpolator());
                resizeAnimation.setDuration(500);
                container.startAnimation(resizeAnimation);
            }
        });

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            // Register callback
            mNfcAdapter.setNdefPushMessageCallback(this, this);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NfcBuddyInfo nfcBuddyInfo = new NfcBuddyInfo(accountType, buddyId, buddyNick, buddyStatus);
        return new NdefMessage(
                new NdefRecord[]{
                        createMime(Settings.MIME_TYPE, GsonSingleton.getInstance().toJson(nfcBuddyInfo)),
                        NdefRecord.createApplicationRecord(getPackageName())
                });
    }

    private NdefRecord createMime(String mimeType, String text) {
        return new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeType.getBytes(), new byte[0], text.getBytes());
    }

    protected abstract int getLayout();

    protected abstract int getDefaultAvatar();

    public String getBuddyName() {
        return buddyNickView.getText().toString();
    }

    protected void updateBuddyNick() {
        String nick = getBuddyName();
        if (TextUtils.isEmpty(nick) || TextUtils.equals(nick, buddyId)) {
            nick = getBuddyNick();
            if (TextUtils.isEmpty(nick)) {
                nick = StringUtil.appendIfNotEmpty(nick, getFirstName(), "");
                nick = StringUtil.appendIfNotEmpty(nick, getLastName(), " ");
            }
        }
        buddyNickView.setText(nick);
    }

    protected void updateBuddyNick(String buddyNick, String fistName, String lastName) {
        setBuddyNick(buddyNick);
        setFirstName(firstName);
        setLastName(lastName);
        buddyNickView.setText("");
        updateBuddyNick();
    }

    private void updateAboutMe() {
        String aboutMe = getAboutMe();
        if (TextUtils.isEmpty(aboutMe)) {
            findViewById(R.id.info_about_me_title).setVisibility(View.GONE);
            findViewById(R.id.about_me).setVisibility(View.GONE);
            findViewById(R.id.info_about_me_footer).setVisibility(View.GONE);
        }
    }

    public abstract void onBuddyInfoRequestError();

    private void hideProgressBar() {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
    }

    private void showProgressBar() {
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onCoreServiceReady() {
        requestBuddyInfo();
    }

    private void requestBuddyInfo() {
        TaskExecutor.getInstance().execute(new BuddyInfoRequestTask(this));
    }

    protected void refreshBuddyInfo() {
        showProgressBar();
        // Hide info blocks.
        setInfoBlocksVisibility(View.GONE);
        // Sending protocol buddy info request.
        requestBuddyInfo();
    }

    private void setInfoBlocksVisibility(int visibility) {
        // Info blocks.
        findViewById(R.id.base_info).setVisibility(visibility);
        findViewById(R.id.extended_info).setVisibility(visibility);
        findViewById(R.id.about_me).setVisibility(visibility);
        // Info titles.
        findViewById(R.id.info_base_title).setVisibility(visibility);
        findViewById(R.id.info_extended_title).setVisibility(visibility);
        findViewById(R.id.info_about_me_title).setVisibility(visibility);
        // Footers
        findViewById(R.id.info_base_footer).setVisibility(visibility);
        findViewById(R.id.info_extended_footer).setVisibility(visibility);
        findViewById(R.id.info_about_me_footer).setVisibility(visibility);
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Check for this is info response and not edit info response.
        boolean isInfoResponse = intent.getBooleanExtra(BuddyInfoRequest.INFO_RESPONSE, false);
        // Check for info present in this intent.
        boolean isInfoPresent = !intent.getBooleanExtra(BuddyInfoRequest.NO_INFO_CASE, false);
        int requestAccountDbId = intent.getIntExtra(BuddyInfoRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        String requestBuddyId = intent.getStringExtra(BuddyInfoRequest.BUDDY_ID);
        // Checking for avatar info received.
        String requestAvatarHash = intent.getStringExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH);
        Logger.log("buddy id: " + requestBuddyId);
        // Checking for this is info we need.
        if (requestAccountDbId == accountDbId && TextUtils.equals(requestBuddyId, buddyId)) {
            // Hide the progress bar.
            hideProgressBar();
            // Checking for avatar hash is new and cool.
            if (!TextUtils.isEmpty(requestAvatarHash) && !TextUtils.equals(requestAvatarHash, avatarHash)) {
                updateAvatar(requestAvatarHash, true);
            }
            // Check for this is info response (not info edit)...
            if (isInfoResponse) {
                // ... and info present.
                if (isInfoPresent) {
                    Bundle bundle = intent.getExtras();
                    // Show info blocks.
                    setInfoBlocksVisibility(View.VISIBLE);
                    // Iterate by info keys.
                    for (String key : bundle.keySet()) {
                        if (StringUtil.isNumeric(key)) {
                            int keyInt = Integer.valueOf(key);
                            String[] extra = intent.getStringArrayExtra(key);
                            // Strange, but... Let's check extra is not empty.
                            if (extra != null && extra.length >= 1) {
                                String title = getString(Integer.parseInt(extra[0]));
                                String value = extra[1];
                                // Prepare buddy info item.
                                View buddyInfoItem = findViewById(keyInt);
                                if (buddyInfoItem != null) {
                                    TextView titleView = (TextView) buddyInfoItem.findViewById(R.id.info_title);
                                    if (titleView != null) {
                                        titleView.setText(title);
                                    }
                                    TextView valueView = (TextView) buddyInfoItem.findViewById(R.id.info_value);
                                    if (valueView != null) {
                                        valueView.setText(value);
                                    }
                                    buddyInfoItem.setVisibility(View.VISIBLE);
                                }
                                // Correct user-defined values for sharing.
                                if (keyInt == R.id.friendly_name) {
                                    setBuddyNick(value);
                                } else if (keyInt == R.id.first_name) {
                                    setFirstName(value);
                                } else if (keyInt == R.id.last_name) {
                                    setLastName(value);
                                } else if (keyInt == R.id.about_me) {
                                    setAboutMe(value);
                                }
                            }
                        }
                    }
                    updateBuddyNick();
                    updateAboutMe();
                } else {
                    Logger.log("No info case :(");
                    onBuddyInfoRequestError();
                }
            }
        } else {
            Logger.log("Wrong buddy info!");
        }
    }

    protected void updateAvatar(String hash, boolean animation) {
        avatarHash = hash;
        ContactImage contactBadgeUpdate = (ContactImage) findViewById(R.id.buddy_image_update);
        if (animation) {
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
            fadeIn.setDuration(750);
            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ContactImage contactBadge = (ContactImage) findViewById(R.id.buddy_image);
                    contactBadge.clearAnimation();
                    BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, getDefaultAvatar(), true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            contactBadgeUpdate.setAnimation(fadeIn);
        } else {
            contactBadgeUpdate.clearAnimation();
        }
        BitmapCache.getInstance().getBitmapAsync(contactBadgeUpdate, avatarHash, getDefaultAvatar(), true);
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

    public String getAboutMe() {
        return aboutMe;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public static class BuddyInfoRequestTask extends ServiceTask<AbstractInfoActivity> {

        public BuddyInfoRequestTask(AbstractInfoActivity object) {
            super(object);
        }

        private String appSession;

        @Override
        public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
            appSession = interaction.getAppSession();
        }

        @Override
        public void onSuccessMain() {
            AbstractInfoActivity activity = getWeakObject();
            if (activity != null) {
                ContentResolver contentResolver = activity.getContentResolver();
                int accountDbId = activity.getAccountDbId();
                String buddyId = activity.getBuddyId();
                boolean accountActive = QueryHelper.isAccountActive(contentResolver, accountDbId);
                if (accountActive) {
                    // Sending protocol buddy info request.
                    RequestHelper.requestBuddyInfo(contentResolver, appSession, accountDbId, buddyId);
                } else {
                    // Account is not active, so we cannot load more info.
                    activity.hideProgressBar();
                }
            }
        }

        @Override
        public void onFailMain() {
            Logger.log("Unable to publish buddy info request due to exception.");
            AbstractInfoActivity activity = getWeakObject();
            if (activity != null) {
                activity.onBuddyInfoRequestError();
            }
        }
    }
}
