package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.views.ContactImage;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by Solkin on 24.03.2015.
 */
public abstract class EditUserInfoActivity extends ChiefActivity implements ChiefActivity.CoreServiceListener {

    private static final int PICK_AVATAR_REQUEST_CODE = 0x01;
    private static final int CROP_AVATAR_REQUEST_CODE = 0x02;
    private static final int LARGE_AVATAR_SIZE = 600;
    private static final String ACTION_CROP = "com.android.camera.action.CROP";

    public static final String ACCOUNT_DB_ID = "account_db_id";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String AVATAR_HASH = "buddy_avatar_hash";

    private int accountDbId;
    private String accountType;
    private String avatarHash;

    private Bitmap manualAvatar;
    private String manualAvatarVirtualHash;

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

        View changeAvatarButton = findViewById(R.id.change_avatar_button);
        changeAvatarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickAvatar();
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
                sendManualBitmap();
                sendEditUserInfoRequest();
                finish();
                return true;
        }
        return false;
    }

    public int getAccountDbId() {
        return accountDbId;
    }

    protected abstract void afterCreate();

    protected abstract int getLayout();

    protected void onUserInfoRequestError() {
        Toast.makeText(this, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
    }

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
            hideProgressBar();
            onUserInfoRequestError();
        }
    }

    @Override
    public void onCoreServiceDown() {

    }

    protected void sendManualBitmap() {
        // Check for manual avatar exists.
        if (manualAvatar != null && !TextUtils.isEmpty(manualAvatarVirtualHash)) {
            // This will cache avatar with specified hash and also for current account.
            TaskExecutor.getInstance().execute(new UpdateAvatarTask(this, accountDbId, manualAvatar, manualAvatarVirtualHash, avatarHash));
        }
    }

    protected abstract void sendManualAvatarRequest(String hash);

    protected abstract void sendEditUserInfoRequest();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_AVATAR_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    onAvatarPicked(data.getData());
                }
                break;
            }
            case CROP_AVATAR_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    TaskExecutor.getInstance().execute(new AvatarSamplingTask(this, data.getData()));
                }
                break;
            }
        }
    }

    private void pickAvatar() {
        try {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, PICK_AVATAR_REQUEST_CODE);
        } catch (Throwable ignored) {
            // No such application?!
        }
    }

    private void onAvatarPicked(Uri uri) {
        try {
            Intent intent = new Intent(ACTION_CROP);
            intent.setData(uri);
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", LARGE_AVATAR_SIZE);
            intent.putExtra("outputY", LARGE_AVATAR_SIZE);
            intent.putExtra("noFaceDetection", true);
            startActivityForResult(intent, CROP_AVATAR_REQUEST_CODE);
        } catch (Throwable ignored) {
            // No such application?!
            TaskExecutor.getInstance().execute(new AvatarSamplingTask(this, uri));
        }
    }

    @SuppressWarnings("deprecation")
    private void setManualAvatar(Bitmap bitmap, String hash) {
        // Apply manual avatar and hash.
        manualAvatar = bitmap;
        manualAvatarVirtualHash = hash;
        // Show new avatar with animation.
        ContactImage contactBadgeManual = (ContactImage) findViewById(R.id.buddy_image_manual);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setDuration(750);
        contactBadgeManual.setAnimation(fadeIn);
        contactBadgeManual.setBitmap(bitmap);
    }

    private void onAvatarChangeError() {
        Toast.makeText(this, R.string.avatar_changing_error, Toast.LENGTH_SHORT).show();
    }

    public static class AvatarSamplingTask extends WeakObjectTask<EditUserInfoActivity> {

        private final Uri uri;
        private Bitmap bitmap;
        private String hash;

        public AvatarSamplingTask(EditUserInfoActivity object, Uri uri) {
            super(object);
            this.uri = uri;
            this.hash = HttpUtil.getUrlHash(uri.toString());
        }

        @Override
        public void executeBackground() throws Throwable {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null) {
                bitmap = BitmapHelper.decodeSampledBitmapFromUri(activity, uri, LARGE_AVATAR_SIZE, LARGE_AVATAR_SIZE);
                if (bitmap == null) {
                    throw new NullPointerException();
                }
            }
        }

        @Override
        public void onSuccessMain() {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null && bitmap != null) {
                activity.setManualAvatar(bitmap, hash);
            }
        }

        @Override
        public void onFailMain() {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null) {
                activity.onAvatarChangeError();
            }
        }
    }

    public static class UpdateAvatarTask extends WeakObjectTask<EditUserInfoActivity> {

        private final Bitmap avatar;
        private final int accountDbId;
        private final String virtualHash;
        private final String avatarHash;

        public UpdateAvatarTask(EditUserInfoActivity object, int accountDbId, Bitmap avatar, String virtualHash, String avatarHash) {
            super(object);
            this.accountDbId = accountDbId;
            this.avatar = avatar;
            this.virtualHash = virtualHash;
            this.avatarHash = avatarHash;
        }

        @Override
        public void executeBackground() throws Throwable {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null) {
                BitmapCache bitmapCache = BitmapCache.getInstance();
                // Remove all cached avatars.
                bitmapCache.invalidateHash(virtualHash);
                bitmapCache.invalidateHash(avatarHash);
                // Same new bitmaps.
                bitmapCache.saveBitmapSync(virtualHash, avatar);
                bitmapCache.saveBitmapSync(avatarHash, avatar);
                // Update profile.
                QueryHelper.updateAccountAvatar(activity.getContentResolver(), accountDbId, avatarHash);
            }
        }

        @Override
        public void onSuccessMain() {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null && avatar != null) {
                activity.sendManualAvatarRequest(virtualHash);
            }
        }

        @Override
        public void onFailMain() {
            EditUserInfoActivity activity = getWeakObject();
            if (activity != null) {
                activity.onAvatarChangeError();
            }
        }
    }
}
