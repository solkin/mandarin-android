package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.im.icq.UserInfoRequest;
import com.tomclaw.mandarin.main.views.ContactImage;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;

import java.lang.ref.WeakReference;

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

    public static final String USER_NICK = "user_nick";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";

    private int accountDbId;
    private String accountType;
    private String avatarHash;

    private Bitmap manualAvatar;
    private String manualAvatarVirtualHash;
    private boolean isInfoReceived;

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
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Initialize info activity layout.
        setContentView(getLayout());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Preparing for action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.account_info);
        }

        // Buddy avatar.
        ContactImage contactBadge = (ContactImage) findViewById(R.id.buddy_image);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.def_avatar_0, false);
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p;
            p = (FrameLayout.LayoutParams) changeAvatarButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0);
            changeAvatarButton.setLayoutParams(p);
        }

        afterCreate();
    }

    @Override
    public void onBackPressed() {
        onActivityCloseAttempt();
    }

    private void onActivityCloseAttempt() {
        // Check for info received and we must notify user about closing activity.
        if (isInfoReceived) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.close_without_user_info_save)
                    .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.no, null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.edit_user_info_menu, menu);
        final MenuItem item = menu.findItem(R.id.edit_user_info_complete);
        TextView actionView = ((TextView) item.getActionView());
        actionView.setText(actionView.getText().toString().toUpperCase());
        actionView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(item.getItemId(), 0);
            }
        });

        if (isInfoReceived) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                onBackPressed();
                return true;
            case R.id.edit_user_info_complete:
                UpdateAvatarTask.UpdateAvatarCallback callback = new UpdateAvatarTask.UpdateAvatarCallback() {
                    @Override
                    public void onUpdateCompleted() {
                        onAvatarUpdateCompleted();
                    }

                    @Override
                    public void onUpdateFailed() {
                        onAvatarChangeError();
                    }
                };
                // Check for manual avatar exists.
                if (manualAvatar != null && !TextUtils.isEmpty(manualAvatarVirtualHash)) {
                    // This will cache avatar with specified hash and also for current account.
                    TaskExecutor.getInstance().execute(new UpdateAvatarTask(this, callback,
                            accountDbId, manualAvatar, manualAvatarVirtualHash, avatarHash));
                } else {
                    onAvatarUpdateCompleted();
                }
                return true;
        }
        return false;
    }

    protected abstract String getUserNick();

    protected abstract String getFirstName();

    protected abstract String getLastName();

    public int getAccountDbId() {
        return accountDbId;
    }

    protected abstract void afterCreate();

    protected abstract int getLayout();

    protected void onUserInfoRequestError() {
        Toast.makeText(this, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
    }

    private void hideProgressBar() {
        ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.content_view_switcher);
        if (viewSwitcher.getDisplayedChild() == 0) {
            viewSwitcher.setDisplayedChild(1);
        }
        // Now we means that info received and user ready to modify info.
        if (!isInfoReceived) {
            isInfoReceived = true;
            invalidateOptionsMenu();
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeAsUpIndicator(R.drawable.ic_close);
            }
        }
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        boolean isEditInfoRequest = intent.getBooleanExtra(UserInfoRequest.EDIT_INFO_REQUEST, false);
        if (isEditInfoRequest) {
            // Check for info present in this intent.
            boolean isInfoPresent = !intent.getBooleanExtra(UserInfoRequest.NO_INFO_CASE, false);
            int requestAccountDbId = intent.getIntExtra(UserInfoRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
            // Checking for avatar info received.
            String requestAvatarHash = intent.getStringExtra(UserInfoRequest.BUDDY_AVATAR_HASH);
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
                    BitmapCache.getInstance().getBitmapAsync(contactBadgeUpdate, avatarHash, R.drawable.def_avatar_0, true);
                }
                if (isInfoPresent) {
                    onUserInfoReceived(intent);
                } else {
                    onUserInfoRequestError();
                }
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
            RequestHelper.requestUserInfo(contentResolver, appSession, accountDbId);
        } catch (Throwable ignored) {
            hideProgressBar();
            onUserInfoRequestError();
        }
    }

    @Override
    public void onCoreServiceDown() {
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
                    Uri uri = data.getData();
                    if (uri == null && !TextUtils.isEmpty(data.getAction())) {
                        uri = Uri.parse(data.getAction());
                    }
                    if (uri != null) {
                        TaskExecutor.getInstance().execute(new AvatarSamplingTask(this, uri));
                    }
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
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ContactImage contactBadgeUpdate = (ContactImage) findViewById(R.id.buddy_image_update);
                // Duplicate avatar for layer below.
                contactBadgeUpdate.setBitmap(manualAvatar);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        contactBadgeManual.setAnimation(fadeIn);
        contactBadgeManual.setBitmap(bitmap);
    }

    private void onAvatarChangeError() {
        Toast.makeText(this, R.string.avatar_changing_error, Toast.LENGTH_SHORT).show();
    }

    private void onAvatarUpdateCompleted() {
        sendEditUserInfoRequest();
        setResult(RESULT_OK, new Intent()
                .putExtra(USER_NICK, getUserNick())
                .putExtra(FIRST_NAME, getFirstName())
                .putExtra(LAST_NAME, getLastName())
                .putExtra(AVATAR_HASH, manualAvatarVirtualHash));
        finish();
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

    public static class UpdateAvatarTask extends PleaseWaitTask {

        private final WeakReference<EditUserInfoActivity> weakActivity;
        private final Bitmap avatar;
        private final UpdateAvatarCallback callback;
        private final int accountDbId;
        private final String virtualHash;
        private final String avatarHash;

        public UpdateAvatarTask(EditUserInfoActivity object, UpdateAvatarCallback callback,
                                int accountDbId, Bitmap avatar, String virtualHash, String avatarHash) {
            super(object);
            weakActivity = new WeakReference<>(object);
            this.callback = callback;
            this.accountDbId = accountDbId;
            this.avatar = avatar;
            this.virtualHash = virtualHash;
            this.avatarHash = TextUtils.isEmpty(avatarHash) ? virtualHash : avatarHash;
        }

        @Override
        public void executeBackground() throws Throwable {
            EditUserInfoActivity activity = weakActivity.get();
            if (activity != null) {
                BitmapCache bitmapCache = BitmapCache.getInstance();
                // Remove all cached avatars.
                bitmapCache.invalidateHash(virtualHash);
                bitmapCache.invalidateHash(avatarHash);
                // Same new bitmaps.
                bitmapCache.saveBitmapSync(virtualHash, avatar);
                bitmapCache.saveBitmapSync(avatarHash, avatar);
                // Update profile.
                QueryHelper.updateAccountAvatar(activity.getContentResolver(), accountDbId, virtualHash);
            }
        }

        @Override
        public void onSuccessMain() {
            EditUserInfoActivity activity = weakActivity.get();
            if (activity != null && avatar != null) {
                activity.sendManualAvatarRequest(avatarHash);
                callback.onUpdateCompleted();
            }
        }

        @Override
        public void onFailMain() {
            EditUserInfoActivity activity = weakActivity.get();
            if (activity != null) {
                activity.onAvatarChangeError();
                callback.onUpdateFailed();
            }
        }

        public interface UpdateAvatarCallback {

            void onUpdateCompleted();

            void onUpdateFailed();
        }
    }
}
