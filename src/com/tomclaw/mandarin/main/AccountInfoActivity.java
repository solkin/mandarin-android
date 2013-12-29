package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
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
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class AccountInfoActivity extends AbstractInfoActivity {

    public static final String BUDDY_STATUS_TITLE = "buddy_status_title";
    public static final String BUDDY_STATUS_MESSAGE = "buddy_status_message";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_info_menu, menu);
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
                    getServiceInteraction().updateAccountStatus(
                            getAccountType(), getBuddyId(), StatusUtil.STATUS_OFFLINE);
                    finish();
                } catch (RemoteException ignored) {
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
                                ChiefActivity chiefActivity = weakChiefActivity.get();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "AccountInfoActivity onCreate");

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
            bar.setTitle(R.string.account_info);
        }

        // Initialize info activity layout.
        setContentView(R.layout.account_info_activity);

        TextView buddyIdView = (TextView) findViewById(R.id.user_id);
        buddyIdView.setText(getBuddyId());

        TextView buddyNickView = (TextView) findViewById(R.id.user_nick);
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
        ImageView contactBadge = (ImageView) findViewById(R.id.user_badge);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, getAvatarHash(), R.drawable.ic_default_avatar);
    }

    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
    }
}
