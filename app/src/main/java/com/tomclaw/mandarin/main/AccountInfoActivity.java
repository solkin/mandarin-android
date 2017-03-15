package com.tomclaw.mandarin.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.icq.IcqEditUserInfoActivity;
import com.tomclaw.mandarin.main.tasks.AccountsRemoveTask;
import com.tomclaw.mandarin.util.Logger;

import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class AccountInfoActivity extends AbstractInfoActivity {

    private static final int REQUEST_USER_INFO_EDIT = 0x09;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuResource;
        if (getBuddyStatus() == StatusUtil.STATUS_OFFLINE) {
            menuResource = R.menu.account_info_offline_menu;
        } else {
            menuResource = R.menu.account_info_menu;
        }
        getMenuInflater().inflate(menuResource, menu);
        prepareShareMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.account_shutdown:
                try {
                    // Trying to disconnect account.
                    getServiceInteraction().updateAccountStatusIndex(
                            getAccountType(), getBuddyId(), StatusUtil.STATUS_OFFLINE);
                    finish();
                } catch (Throwable ignored) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(this, R.string.unable_to_shutdown_account, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.account_remove:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.remove_account_title);
                builder.setMessage(R.string.remove_account_text);
                builder.setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Collection<Integer> selectedAccounts = Collections.singleton(getAccountDbId());
                        AccountsRemoveTask task = new AccountsRemoveTask(AccountInfoActivity.this, selectedAccounts) {

                            @Override
                            public void onSuccessMain() {
                                ChiefActivity chiefActivity = getChiefActivity();
                                if (chiefActivity != null) {
                                    chiefActivity.finish();
                                }
                            }
                        };
                        TaskExecutor.getInstance().execute(task);
                    }
                });
                builder.setNegativeButton(R.string.do_not_remove, null);
                builder.show();
                return true;
            case R.id.account_instance:
                RequestHelper.leaveOtherSessions(getContentResolver(), getAccountDbId());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("AccountInfoActivity onCreate");

        // Preparing for action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.account_info);
        }

        View editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editUserInfo();
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p;
            p = (FrameLayout.LayoutParams) editButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0);
            editButton.setLayoutParams(p);
        }
    }

    private void editUserInfo() {
        startActivityForResult(new Intent(this, IcqEditUserInfoActivity.class)
                        .putExtra(IcqEditUserInfoActivity.ACCOUNT_DB_ID, getAccountDbId())
                        .putExtra(IcqEditUserInfoActivity.ACCOUNT_TYPE, getAccountType())
                        .putExtra(IcqEditUserInfoActivity.AVATAR_HASH, getAvatarHash()),
                REQUEST_USER_INFO_EDIT
        );
    }

    @Override
    protected int getLayout() {
        return R.layout.account_info_activity;
    }

    @Override
    protected int getDefaultAvatar() {
        return R.drawable.def_avatar_0;
    }

    @Override
    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_USER_INFO_EDIT) {
            if (resultCode == RESULT_OK) {
                // Obtain fresh info.
                String buddyNick = data.getStringExtra(EditUserInfoActivity.USER_NICK);
                String firstName = data.getStringExtra(EditUserInfoActivity.FIRST_NAME);
                String lastName = data.getStringExtra(EditUserInfoActivity.LAST_NAME);
                String avatarHash = data.getStringExtra(EditUserInfoActivity.AVATAR_HASH);
                // Update buddy nick.
                updateBuddyNick(buddyNick, firstName, lastName);
                // Check for avatar changed.
                if (!TextUtils.isEmpty(avatarHash)) {
                    // Update avatar.
                    setAvatarImmutable(false);
                    updateAvatar(avatarHash, false);
                    setAvatarImmutable(true);
                }
                // Re-request buddy info from server.
                refreshBuddyInfo();
            }
        }
    }
}
