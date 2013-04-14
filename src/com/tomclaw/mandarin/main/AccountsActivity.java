package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;

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
        return super.onOptionsItemSelected(item);
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
}
