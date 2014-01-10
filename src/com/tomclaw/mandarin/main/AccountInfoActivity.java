package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.adapters.StatusSpinnerAdapter;

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
    public static final String STATE_REQUESTED = "state_requested";
    public static final String STATE_APPLIED = "state_applied";
    public static final String SET_STATE_SUCCESS = "set_state_success";

    private TextView statusTextView;
    private Spinner statusSpinner;
    private StatusSpinnerAdapter spinnerAdapter;

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
                    getServiceInteraction().updateAccountStatusIndex(
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

        spinnerAdapter = new StatusSpinnerAdapter(this, getAccountType(),
                StatusUtil.getSetupStatuses(getAccountType()));

        statusSpinner = (Spinner) findViewById(R.id.status_spinner);
        statusSpinner.setAdapter(spinnerAdapter);

        if (!TextUtils.isEmpty(getAccountType())) {
            String statusString;
            // Status text.
            if (TextUtils.isEmpty(buddyStatusMessage)
                    && !TextUtils.equals(buddyStatusTitle, StatusUtil.getStatusTitle(getAccountType(), getBuddyStatus()))) {
                // Account status message is empty, but status title don't
                // and title is not a default title. Let's show status title
                // instead empty status message.
                statusString = buddyStatusTitle;
            } else {
                statusString = buddyStatusMessage;
            }
            statusTextView = ((TextView) findViewById(R.id.status_text));
            statusTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onStatusTextEditClick();
                }
            });
            statusTextView.setText(statusString);
            // Setup selected status in spinner.
            try {
                statusSpinner.setSelection(spinnerAdapter.getStatusPosition(getBuddyStatus()), false);
            } catch (StatusNotFoundException ignored) {
                // Nothing to do in this case. This may ne produced by incorrect setup status collection.
                Log.d(Settings.LOG_TAG, "Status not found in account info: " + getBuddyStatus());
            }
        }

        // Setup listener after status spinner preparing to prevent extra callbacks.
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onStatusIndexSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Buddy avatar.
        ImageView contactBadge = (ImageView) findViewById(R.id.user_badge);
        BitmapCache.getInstance().getBitmapAsync(contactBadge, getAvatarHash(), R.drawable.ic_default_avatar);
    }

    private void onStatusTextEditClick() {
        final CharSequence statusMessageBefore = statusTextView.getText();
        // Preparing dialog content.
        final EditText input = new EditText(this);
        input.setText(statusMessageBefore);
        // Building dialog.
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.status_message)
                .setView(input)
                .create();
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.apply), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                CharSequence statusMessageAfter = input.getText();
                // Checking for status message is not empty and was changed.
                if (statusMessageAfter != null
                        && !TextUtils.equals(statusMessageAfter, statusMessageBefore)) {
                    onStatusMessageChanged(statusMessageAfter.toString());
                }
                // Closing dialog.
                dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.clear), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                onStatusMessageChanged("");
                // Closing dialog.
                dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.not_now), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Closing dialog.
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
    }

    private void onStatusIndexSelected(int position) {
        Log.d(Settings.LOG_TAG, "Status selected: [position: " + position + "]");
        try {
            getServiceInteraction().updateAccountStatusIndex(getAccountType(), getBuddyId(),
                    spinnerAdapter.getStatus(position));
            statusTextView.setText("");
        } catch (RemoteException ignored) {
            Log.d(Settings.LOG_TAG, "Unable to setup status due to remote exception");
            Toast.makeText(AccountInfoActivity.this, R.string.unable_to_setup_status, Toast.LENGTH_SHORT).show();
        }
    }

    private void onStatusMessageChanged(String statusMessage) {
        try {
            int position = statusSpinner.getSelectedItemPosition();
            int statusIndex = spinnerAdapter.getStatus(position);
            getServiceInteraction().updateAccountStatus(
                    getAccountType(), getBuddyId(), statusIndex,
                    StatusUtil.getStatusTitle(getAccountType(), statusIndex),
                    statusMessage);
            statusTextView.setText(statusMessage);
        } catch (RemoteException ignored) {
            Log.d(Settings.LOG_TAG, "Error while status message changing.");
        }
    }
}
