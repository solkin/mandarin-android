package com.tomclaw.mandarin.main;

import android.content.Intent;
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
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class AccountsActivity extends ChiefActivity implements
        ActionBar.OnNavigationListener {

    public static final int ADDING_ACTIVITY_RESULT_CODE = 1;
    protected boolean mActionMode;
    protected int selectedItem;
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
                    show();
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                case R.id.remove_account_menu:
                    show();
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
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.accounts);
        // Initialize accounts list
        initAccountsList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account_menu:
                Intent intent = new Intent(this, AccountAddActivity.class);
                intent.putExtra(AccountAddActivity.CLASS_NAME_EXTRA, IcqAccountRoot.class.getName());
                startActivityForResult(intent, ADDING_ACTIVITY_RESULT_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initAccountsList() {
        try {
            List<AccountRoot> accountsList = getServiceInteraction().getAccountsList();
            Log.d(Settings.LOG_TAG, "received " + accountsList.size() + " accounts");
            // Set up list as default container.
            setContentView(R.layout.accounts_list);
            // Checking for there is no accounts.
            if (accountsList.isEmpty()) {
                // Nothing to do in this case.
                Log.d(Settings.LOG_TAG, "No accounts");
            } else {
                ListView listView = (ListView) findViewById(R.id.listView);
                // Creating adapter for accounts list
                AccountsAdapter sAdapter = new AccountsAdapter(this, R.layout.account_item, accountsList);
                // Bind to our new adapter.
                listView.setAdapter(sAdapter);
                listView.setItemsCanFocus(true);
                listView.setOnItemClickListener(new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        startActivity(new Intent(AccountsActivity.this, SummaryActivity.class));
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (mActionMode == true) {
                            return false;
                        }
                        selectedItem = position;

                        // Start the CAB using the ActionMode.Callback defined above
                        mActionMode = true;
                        AccountsActivity.this.startActionMode(mActionModeCallback);
                        view.setSelected(true);
                        return true;
                    }
                });
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    private void show() {
        Toast.makeText(AccountsActivity.this,
                String.valueOf(selectedItem), Toast.LENGTH_LONG).show();
    }
}
