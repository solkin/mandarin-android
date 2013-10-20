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
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.adapters.AccountsAdapter;
import com.tomclaw.mandarin.main.adapters.StatusSpinnerAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.StatusUtil;

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
    private ListView accountsList;

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
                Intent mainActivityIntent = new Intent(this, MainActivity.class);
                startActivity(mainActivityIntent);
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
        accountsList = (ListView) findViewById(R.id.accounts_list_wiew);
        // Creating adapter for accounts list
        accountsAdapter = new AccountsAdapter(this, getLoaderManager());
        // Bind to our new adapter.
        accountsList.setAdapter(accountsAdapter);
        accountsList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        accountsList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // startActivity(new Intent(AccountsActivity.this, SummaryActivity.class));
                Cursor cursor = accountsAdapter.getCursor();
                if (cursor.moveToPosition(position)) {
                    int COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
                    int COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
                    int COLUMN_ACCOUNT_STATUS = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS);
                    final String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
                    final String userId = cursor.getString(COLUMN_USER_ID);
                    final int statusIndex = cursor.getInt(COLUMN_ACCOUNT_STATUS);

                    // Checking for account is offline and we need to connect.
                    if(statusIndex == StatusUtil.STATUS_OFFLINE) {
                        View connectDialog = getLayoutInflater().inflate(R.layout.connect_dialog, null);
                        final Spinner statusSpinner = (Spinner) connectDialog.findViewById(R.id.status_spinner);

                        final StatusSpinnerAdapter spinnerAdapter =
                                new StatusSpinnerAdapter(AccountsActivity.this, accountType);
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
                                    getServiceInteraction().updateAccountStatus(
                                            accountType, userId, selectedStatusIndex);
                                } catch (RemoteException e) {
                                    // Heh... Nothing to do in this case.
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.connect_no, null);
                        builder.show();
                    } else {
                        // Debug purposes only.
                        try {
                            // Trying to connect account.
                            getServiceInteraction().updateAccountStatus(
                                    accountType, userId, StatusUtil.STATUS_OFFLINE);
                        } catch (RemoteException e) {
                            // Heh... Nothing to do in this case.
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

    private void show(int stringRes) {
        Toast.makeText(AccountsActivity.this, stringRes, Toast.LENGTH_LONG).show();
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            selectionHelper = new SelectionHelper();
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
                            Cursor cursor = accountsAdapter.getCursor();
                            // Obtain selected positions.
                            Collection<Integer> selectedPositions = selectionHelper.getSelectedPositions();
                            int positionsBeingRemoved = selectedPositions.size();
                            // Iterating for all selected positions.
                            for(int position : selectedPositions) {
                                // Checking for position available.
                                if (cursor.moveToPosition(position)) {
                                    // Detecting columns.
                                    int COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
                                    int COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
                                    String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
                                    String userId = cursor.getString(COLUMN_USER_ID);
                                    try {
                                        // Trying to remove account.
                                        if (getServiceInteraction().removeAccount(accountType, userId)) {
                                            // Position successfully removed.
                                            positionsBeingRemoved--;
                                        }
                                    } catch (RemoteException ignored) {
                                        // Heh... Nothing to do in this case.
                                    }
                                }
                            }
                            // Checking for something is not removed.
                            if(positionsBeingRemoved > 0) {
                                // Show error.
                                show(R.string.error_no_such_account);
                            }
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
