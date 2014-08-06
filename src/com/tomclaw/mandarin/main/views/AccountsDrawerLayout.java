package com.tomclaw.mandarin.main.views;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.SettingsActivity;
import com.tomclaw.mandarin.main.adapters.AccountsAdapter;
import com.tomclaw.mandarin.main.adapters.StatusSpinnerAdapter;
import com.tomclaw.mandarin.main.tasks.AccountInfoTask;
import com.tomclaw.mandarin.main.tasks.AccountsRemoveTask;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by solkin on 24/04/14.
 */
public class AccountsDrawerLayout extends DrawerLayout {

    private ChiefActivity activity;
    private AccountsAdapter accountsAdapter;
    private ActionBarDrawerToggle drawerToggle;
    private LinearLayout drawerContent;
    private CharSequence title;
    private CharSequence drawerTitle;

    public AccountsDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(final ChiefActivity activity) {
        this.activity = activity;
        setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        drawerContent = (LinearLayout) findViewById(R.id.left_drawer);

        final ActionBar actionBar = activity.getActionBar();
        drawerToggle = new ActionBarDrawerToggle(activity, this,
                R.drawable.ic_drawer, R.string.dialogs, R.string.accounts) {

            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionBar.setTitle(title);
                activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                actionBar.setTitle(drawerTitle);
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
                try {
                    activity.getServiceInteraction().connectAccounts();
                } catch (Throwable ignored) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(activity, R.string.unable_to_connect_account,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        final View.OnClickListener disconnectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    activity.getServiceInteraction().disconnectAccounts();
                } catch (RemoteException e) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(activity, R.string.unable_to_shutdown_account,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        Button settingsButton = (Button) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                activity.startActivity(intent);
                closeAccountsPanel();
            }
        });
        // Accounts list.
        ListView accountsList = (ListView) findViewById(R.id.accounts_list_view);
        // Creating adapter for accounts list.
        accountsAdapter = new AccountsAdapter(activity, activity.getLoaderManager());
        accountsAdapter.setOnAvatarClickListener(new AccountsAdapter.OnAvatarClickListener() {

            @Override
            public void onAvatarClicked(int accountDbId, boolean isConnected) {
                if (isConnected) {
                    // Account is online and we can show it's brief info.
                    final AccountInfoTask accountInfoTask =
                            new AccountInfoTask(activity, accountDbId);
                    TaskExecutor.getInstance().execute(accountInfoTask);
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
        accountsList.setMultiChoiceModeListener(new AccountsMultiChoiceModeListener());
        accountsList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = accountsAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    final int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                    final String accountType = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE));
                    final String userId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID));
                    final int statusIndex = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS));
                    final String statusTitle = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS_TITLE));
                    final String statusMessage = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS_MESSAGE));
                    final int accountConnecting = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ACCOUNT_CONNECTING));

                    // Checking for account is connecting now and we must wait for some time.
                    if (accountConnecting == 1) {
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
            }
        });
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
                try {
                    int selectedStatusIndex = spinnerAdapter.getStatus(
                            statusSpinner.getSelectedItemPosition());
                    // Trying to connect account.
                    activity.getServiceInteraction().updateAccountStatusIndex(
                            accountType, userId, selectedStatusIndex);
                } catch (Throwable ignored) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(activity, R.string.unable_to_connect_account,
                            Toast.LENGTH_SHORT).show();
                }
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
            Log.d(Settings.LOG_TAG, "Status not found in account info: " + userStatusIndex);
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
                try {
                    int selectedStatusIndex = spinnerAdapter.getStatus(
                            statusSpinner.getSelectedItemPosition());
                    String statusTitleString = StatusUtil.getStatusTitle(accountType, selectedStatusIndex);
                    String statusMessageString = statusMessage.getText().toString();
                    // Trying to update account status.
                    activity.getServiceInteraction().updateAccountStatus(
                            accountType, userId, selectedStatusIndex,
                            statusTitleString, statusMessageString);
                } catch (Throwable ignored) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(activity, R.string.unable_to_connect_account,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.connect_no, null);
        builder.show();
    }

    private class AccountsMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Integer> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, (int) id, checked);
            mode.setTitle(String.format(activity.getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            selectionHelper = new SelectionHelper<Integer, Integer>();
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.accounts_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.remove_account_menu:
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.remove_accounts_title);
                    builder.setMessage(R.string.remove_accounts_text);
                    builder.setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Collection<Integer> selectedAccounts = new ArrayList<Integer>(selectionHelper.getSelectedIds());
                            AccountsRemoveTask task = new AccountsRemoveTask(activity, selectedAccounts);
                            TaskExecutor.getInstance().execute(task);
                            // Action picked, so close the CAB
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(R.string.do_not_remove, null);
                    builder.show();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }
    }
}
