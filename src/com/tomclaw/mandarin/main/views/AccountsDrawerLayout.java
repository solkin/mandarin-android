package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.ServiceInteraction;
import com.tomclaw.mandarin.core.ServiceTask;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.main.adapters.AccountsAdapter;
import com.tomclaw.mandarin.main.adapters.StatusSpinnerAdapter;
import com.tomclaw.mandarin.main.icq.IntroActivity;
import com.tomclaw.mandarin.main.tasks.AccountInfoTask;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by solkin on 24/04/14.
 */
public class AccountsDrawerLayout extends DrawerLayout {

    private ChiefActivity activity;
    private AccountsAdapter accountsAdapter;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence title;
    private CharSequence drawerTitle;

    public AccountsDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(final MainActivity activity, final Toolbar toolbar) {
        this.activity = activity;
        setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        drawerToggle = new ActionBarDrawerToggle(activity, this,
                toolbar, R.string.dialogs, R.string.accounts) {

            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                toolbar.setTitle(title);
                activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                toolbar.setTitle(drawerTitle);
                activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        setDrawerListener(drawerToggle);

        // Buttons.
        final Button connectionButton = (Button) findViewById(R.id.connection_button);
        final View.OnClickListener connectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskExecutor.getInstance().execute(new ServiceTask<ChiefActivity>(activity) {
                    @Override
                    public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
                        interaction.connectAccounts();
                    }
                });
            }
        };
        final View.OnClickListener disconnectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskExecutor.getInstance().execute(new ServiceTask<ChiefActivity>(activity) {
                    @Override
                    public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
                        interaction.disconnectAccounts();
                    }
                });
            }
        };
        Button addAccountButton = (Button) findViewById(R.id.add_account_button);
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent accountAddIntent = new Intent(activity, IntroActivity.class);
                activity.startActivity(accountAddIntent);
                closeAccountsPanel();
            }
        });
        // Accounts list.
        ListView accountsList = (ListView) findViewById(R.id.accounts_list_view);
        // Creating adapter for accounts list.
        accountsAdapter = new AccountsAdapter(activity, activity.getLoaderManager());
        accountsAdapter.setOnAccountClickListener(new AccountsAdapter.OnAccountClickListener() {

            @Override
            public void onAccountClicked(int accountDbId, boolean isConnecting) {
                if (!isConnecting) {
                    // Account is online or offline and we can show it's brief info.
                    final AccountInfoTask accountInfoTask =
                            new AccountInfoTask(activity, accountDbId);
                    TaskExecutor.getInstance().execute(accountInfoTask);
                    closeAccountsPanel();
                }
            }
        });
        accountsAdapter.setOnStatusClickListener(new AccountsAdapter.OnStatusClickListener() {
            @Override
            public void onStatusClicked(int accountDbId, String accountType, String userId, int statusIndex,
                                        String statusTitle, String statusMessage, boolean isConnecting) {
                // Checking for account is connecting now and we must wait for some time.
                if (isConnecting) {
                    int toastMessage;
                    if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                        toastMessage = R.string.account_shutdowning;
                    } else {
                        toastMessage = R.string.account_connecting;
                    }
                    Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show();
                } else {
                    // Checking for account is offline and we need to connect.
                    if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                        showConnectionDialog(accountType, userId);
                    } else {
                        // Account is online and we can change status.
                        showChangeStatusDialog(accountType, userId, statusIndex, statusTitle, statusMessage);
                    }
                    closeAccountsPanel();
                }
            }
        });
        accountsAdapter.setOnAccountsStateListener(new AccountsAdapter.OnAccountsStateListener() {

            @Override
            public void onAccountsStateChanged(final AccountsState state) {
                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int buttonText;
                        boolean buttonEnabled;
                        boolean buttonVisibility;
                        View.OnClickListener buttonListener;
                        switch (state) {
                            case NoAccounts: {
                                buttonText = R.string.accounts;
                                buttonEnabled = false;
                                buttonVisibility = false;
                                buttonListener = null;
                                break;
                            }
                            case Offline: {
                                buttonText = R.string.accounts_connect;
                                buttonEnabled = true;
                                buttonVisibility = true;
                                buttonListener = connectListener;
                                break;
                            }
                            case Online: {
                                buttonText = R.string.accounts_shutdown;
                                buttonEnabled = true;
                                buttonVisibility = true;
                                buttonListener = disconnectListener;
                                break;
                            }
                            case Connecting: {
                                buttonText = R.string.connecting;
                                buttonEnabled = false;
                                buttonVisibility = true;
                                buttonListener = null;
                                break;
                            }
                            default: {
                                buttonText = R.string.disconnecting;
                                buttonEnabled = false;
                                buttonVisibility = true;
                                buttonListener = null;
                            }
                        }
                        connectionButton.setText(buttonText);
                        connectionButton.setEnabled(buttonEnabled);
                        connectionButton.setVisibility(buttonVisibility ? VISIBLE : GONE);
                        connectionButton.setOnClickListener(buttonListener);
                    }
                });
            }
        });
        // Bind to our new adapter.
        accountsList.setAdapter(accountsAdapter);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDrawerTitle(String drawerTitle) {
        this.drawerTitle = drawerTitle;
    }

    public void syncToggleState() {
        drawerToggle.syncState();
    }

    public void onToggleConfigurationChanged(Configuration newConfig) {
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onToggleOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item);
    }

    private void closeAccountsPanel() {
        closeDrawers();
    }

    public void showConnectionDialog(final String accountType, final String userId) {
        View connectionView = LayoutInflater.from(activity).inflate(R.layout.connect_dialog, null);
        final Spinner statusSpinner = (Spinner) connectionView.findViewById(R.id.status_spinner);

        final StatusSpinnerAdapter spinnerAdapter = new StatusSpinnerAdapter(
                getContext(), accountType, StatusUtil.getConnectStatuses(accountType));
        statusSpinner.setAdapter(spinnerAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.connect_account_title);
        builder.setView(connectionView);
        builder.setPositiveButton(R.string.connect_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedStatusIndex = spinnerAdapter.getStatus(
                        statusSpinner.getSelectedItemPosition());
                TaskExecutor.getInstance().execute(new ServiceTask<ChiefActivity>(activity) {
                    @Override
                    public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
                        // Trying to connect account.
                        interaction.updateAccountStatusIndex(
                                accountType, userId, selectedStatusIndex);
                    }
                });
            }
        });
        builder.setNegativeButton(R.string.connect_no, null);
        builder.show();
    }

    public void showChangeStatusDialog(final String accountType, final String userId,
                                       int userStatusIndex, String userStatusTitle, String userStatusMessage) {
        View changeStatusView = LayoutInflater.from(activity).inflate(R.layout.change_status_dialog, null);
        final Spinner statusSpinner = (Spinner) changeStatusView.findViewById(R.id.status_spinner);
        final EditText statusMessage = (EditText) changeStatusView.findViewById(R.id.status_message_edit);

        final StatusSpinnerAdapter spinnerAdapter = new StatusSpinnerAdapter(
                getContext(), accountType, StatusUtil.getSetupStatuses(accountType));
        statusSpinner.setAdapter(spinnerAdapter);
        // Setup selected status in spinner.
        try {
            statusSpinner.setSelection(spinnerAdapter.getStatusPosition(userStatusIndex), false);
        } catch (StatusNotFoundException ignored) {
            // Nothing to do in this case. This may be produced by incorrect setup status collection.
            Logger.log("Status not found in account info: " + userStatusIndex);
        }
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                statusMessage.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String statusString;
        // Status text.
        if (TextUtils.isEmpty(userStatusMessage)
                && !TextUtils.equals(userStatusTitle, StatusUtil.getStatusTitle(accountType, userStatusIndex))) {
            // Account status message is empty, but status title don't
            // and title is not a default title. Let's show status title
            // instead empty status message.
            statusString = userStatusTitle;
        } else {
            statusString = userStatusMessage;
        }
        statusMessage.setText(statusString);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.select_status_title);
        builder.setView(changeStatusView);
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedStatusIndex = spinnerAdapter.getStatus(
                        statusSpinner.getSelectedItemPosition());
                final String statusTitleString = StatusUtil.getStatusTitle(accountType, selectedStatusIndex);
                final String statusMessageString = statusMessage.getText().toString();
                TaskExecutor.getInstance().execute(new ServiceTask<ChiefActivity>(activity) {
                    @Override
                    public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
                        // Trying to update account status.
                        interaction.updateAccountStatus(
                                accountType, userId, selectedStatusIndex,
                                statusTitleString, statusMessageString);
                    }
                });
            }
        });
        builder.setNegativeButton(R.string.connect_no, null);
        builder.show();
    }
}
