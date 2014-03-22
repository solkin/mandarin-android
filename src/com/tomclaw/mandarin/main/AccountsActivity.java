package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.adapters.AccountsAdapter;
import com.tomclaw.mandarin.main.adapters.StatusSpinnerAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 11:11 AM
 */
public class AccountsActivity extends ChiefActivity {

    public static final int ADDING_ACTIVITY_REQUEST_CODE = 1;
    private AccountsAdapter accountsAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.accounts_list_menu, menu);
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.accounts);
        // Initialize accounts list
        initAccountsList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.add_account_menu:
                Intent accountAddIntent = new Intent(this, AccountAddActivity.class);
                accountAddIntent.putExtra(AccountAddActivity.EXTRA_CLASS_NAME, IcqAccountRoot.class.getName());
                startActivityForResult(accountAddIntent, ADDING_ACTIVITY_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainActivityIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Settings.LOG_TAG, "AccountsActivity result code: " + resultCode);
        switch (resultCode) {
            case RESULT_OK: {
                initAccountsList();
                break;
            }
        }
    }

    private void initAccountsList() {
        // Set up list as default container.
        setContentView(R.layout.accounts_list);
        ListView accountsList = (ListView) findViewById(R.id.accounts_list_wiew);
        // Creating adapter for accounts list
        accountsAdapter = new AccountsAdapter(this, getLoaderManager());
        // Bind to our new adapter.
        accountsList.setAdapter(accountsAdapter);
        accountsList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        accountsList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = accountsAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    final int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                    final String accountType = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE));
                    final String userId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID));
                    final int statusIndex = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS));
                    final int accountConnecting = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ACCOUNT_CONNECTING));

                    // Checking for account is connecting now and we must wait for some time.
                    if (accountConnecting == 1) {
                        int toastMessage;
                        if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                            toastMessage = R.string.account_shutdowning;
                        } else {
                            toastMessage = R.string.account_connecting;
                        }
                        Toast.makeText(AccountsActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                    } else {
                        // Checking for account is offline and we need to connect.
                        if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                            View connectDialog = getLayoutInflater().inflate(R.layout.connect_dialog, null);
                            final Spinner statusSpinner = (Spinner) connectDialog.findViewById(R.id.status_spinner);

                            final StatusSpinnerAdapter spinnerAdapter = new StatusSpinnerAdapter(
                                    AccountsActivity.this, accountType, StatusUtil.getConnectStatuses(accountType));
                            statusSpinner.setAdapter(spinnerAdapter);

                            AlertDialog.Builder builder = new AlertDialog.Builder(AccountsActivity.this);
                            builder.setTitle(R.string.connect_account_title);
                            builder.setMessage(R.string.connect_account_message);
                            builder.setView(connectDialog);
                            builder.setPositiveButton(R.string.connect_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        int selectedStatusIndex = spinnerAdapter.getStatus(
                                                statusSpinner.getSelectedItemPosition());
                                        // Trying to connect account.
                                        getServiceInteraction().updateAccountStatusIndex(
                                                accountType, userId, selectedStatusIndex);
                                    } catch (RemoteException e) {
                                        // Heh... Nothing to do in this case.
                                        Toast.makeText(AccountsActivity.this, R.string.unable_to_connect_account,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.connect_no, null);
                            builder.show();
                        } else {
                            // Account is online and we can show it's brief info.
                            final AccountInfoTask accountInfoTask =
                                    new AccountInfoTask(AccountsActivity.this, accountDbId);
                            TaskExecutor.getInstance().execute(accountInfoTask);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Integer> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, (int) id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(AccountsActivity.this);
                    builder.setTitle(R.string.remove_accounts_title);
                    builder.setMessage(R.string.remove_accounts_text);
                    builder.setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Collection<Integer> selectedAccounts = new ArrayList<Integer>(selectionHelper.getSelectedIds());
                            AccountsRemoveTask task = new AccountsRemoveTask(AccountsActivity.this, selectedAccounts);
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
