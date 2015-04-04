package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.views.ContactImage;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Solkin on 24.03.2015.
 */
public abstract class EditUserInfoActivity extends ChiefActivity implements ChiefActivity.CoreServiceListener {


    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String AVATAR_HASH = "buddy_avatar_hash";

    private int accountDbId;
    private String accountType;
    private String avatarHash;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("AbstractInfoActivity onCreate");
        // We want to receive service state notifications.
        addCoreServiceListener(this);

        // Obtain and check basic info about interested buddy.
        Intent intent = getIntent();
        accountDbId = intent.getIntExtra(ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        accountType = intent.getStringExtra(ACCOUNT_TYPE);
        avatarHash = intent.getStringExtra(AVATAR_HASH);

        // Check for required fields are correct.
        if (accountDbId == GlobalProvider.ROW_INVALID) {
            // Nothing we can do in this case. Only show toast and close activity.
            onUserInfoRequestError();
            finish();
            return;
        }

        // Initialize info activity layout.
        setContentView(getLayout());

        // Preparing for action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.account_info);
        }

        // Buddy avatar.
        ContactImage contactBadge = (ContactImage) findViewById(R.id.buddy_image);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar, false);
        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View container = findViewById(R.id.buddy_image_container);
                int targetHeight;
                if(container.getWidth() != container.getHeight()) {
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

        afterCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_user_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.edit_user_info_complete:
                sendEditUserInfoRequest();
                finish();
                return true;
        }
        return false;
    }

    protected abstract void afterCreate();

    protected abstract int getLayout();

    protected abstract void onUserInfoRequestError();

    private void hideProgressBar() {
        // findViewById(R.id.progress_bar).setVisibility(View.GONE);
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Check for info present in this intent.
        boolean isInfoPresent = !intent.getBooleanExtra(BuddyInfoRequest.NO_INFO_CASE, false);
        int requestAccountDbId = intent.getIntExtra(BuddyInfoRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        // Checking for avatar info received.
        String requestAvatarHash = intent.getStringExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH);
        // Checking for this is info we need.
        if (requestAccountDbId == accountDbId) {
            // Hide the progress bar.
            hideProgressBar();
            // Checking for avatar hash is new and cool.
            if (!TextUtils.isEmpty(requestAvatarHash) && !TextUtils.equals(requestAvatarHash, avatarHash)) {
                avatarHash = requestAvatarHash;
                ContactImage contactBadgeUpdate = (ContactImage) findViewById(R.id.buddy_image_update);
                Animation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeIn.setDuration(750);
                contactBadgeUpdate.setAnimation(fadeIn);
                BitmapCache.getInstance().getBitmapAsync(contactBadgeUpdate, avatarHash, R.drawable.ic_default_avatar, true);
            }
            if (isInfoPresent) {
                onUserInfoReceived(intent);
            } else {
                onUserInfoRequestError();
            }
        }
    }

    protected abstract void onUserInfoReceived(Intent intent);

    @Override
    public void onCoreServiceReady() {
        try {
            String appSession = getServiceInteraction().getAppSession();
            ContentResolver contentResolver = getContentResolver();
            // Sending protocol buddy info request.
            RequestHelper.requestAccountInfo(contentResolver, appSession, accountDbId);
        } catch (Throwable ignored) {
            onUserInfoRequestError();
        }
    }

    @Override
    public void onCoreServiceDown() {

    }

    protected abstract void sendEditUserInfoRequest();
}
