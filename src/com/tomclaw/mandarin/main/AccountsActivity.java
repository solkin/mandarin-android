package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.adapters.AccountsAdapter;
import com.tomclaw.mandarin.util.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 11:11 AM
 */
public class AccountsActivity extends ChiefActivity {

    public static final int ADDING_ACTIVITY_REQUEST_CODE = 1;
    protected int selectedItem;
    private AccountsAdapter sAdapter;
    protected boolean mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.accounts_edit_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.edit_account_menu:
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                case R.id.remove_account_menu:
                    Cursor cursor = sAdapter.getCursor();
                    if (cursor.moveToPosition(selectedItem)) {
                        // Detecting columns.
                        int COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
                        int COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
                        String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
                        String userId = cursor.getString(COLUMN_USER_ID);
                        try {
                            // Trying to remove account.
                            if (getServiceInteraction().removeAccount(accountType, userId)) {
                                // Action picked, so close the CAB
                                mode.finish();
                                return true;
                            }
                        } catch (RemoteException e) {
                            // Heh... Nothing to do in this case.
                        }
                    }
                    // Show error.
                    show(R.string.error_no_such_account);
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = false;
            selectedItem = -1;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.accounts_list_menu, menu);
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getSupportActionBar();
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
                accountAddIntent.putExtra(AccountAddActivity.CLASS_NAME_EXTRA, IcqAccountRoot.class.getName());
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
        ListView listView = (ListView) findViewById(R.id.accounts_list_wiew);
        // Creating adapter for accounts list
        sAdapter = new AccountsAdapter(this, getSupportLoaderManager());
        // Bind to our new adapter.
        listView.setAdapter(sAdapter);
        listView.setItemsCanFocus(true);
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Update selected item first.
                selectedItem = position;
                // startActivity(new Intent(AccountsActivity.this, SummaryActivity.class));
                Cursor cursor = sAdapter.getCursor();
                if (cursor.moveToPosition(selectedItem)) {
                    int COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
                    int COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
                    int COLUMN_ACCOUNT_STATUS = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS);
                    String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
                    String userId = cursor.getString(COLUMN_USER_ID);
                    int statusIndex = cursor.getInt(COLUMN_ACCOUNT_STATUS);
                    try {
                        // Trying to connect account.
                        getServiceInteraction().updateAccountStatus(accountType, userId,
                                statusIndex == StatusUtil.STATUS_OFFLINE ?
                                        StatusUtil.STATUS_ONLINE : StatusUtil.STATUS_OFFLINE);
                    } catch (RemoteException e) {
                        // Heh... Nothing to do in this case.
                    }
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Update selected item first.
                selectedItem = position;
                // Checking for action mode is already activated.
                if (mActionMode) {
                    return false;
                }
                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = true;
                startActionMode(mActionModeCallback);
                view.setSelected(true);
                return true;
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
}
