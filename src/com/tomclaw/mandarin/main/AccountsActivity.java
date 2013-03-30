package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Initialize accounts list
        initAccountsList();
    }

    private void initAccountsList() {
        try {
            List<AccountRoot> accountsList = getServiceInteraction().getAccountsList();
            Log.d("MandarinLog", "received " + accountsList.size() + " accounts");
            // Set up list as default container.
            setContentView(R.layout.accounts_list);
            // Checking for there is no accounts.
            if (accountsList.isEmpty()) {
                // Nothing to do in this case.
                Log.d("MandarinLog", "No accounts");
            } else {
                ListView listView = (ListView) findViewById(R.id.listView);

                final String ATTRIBUTE_NAME_TEXT = "text";
                final String ATTRIBUTE_NAME_IMAGE = "image";

                List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                for (AccountRoot accountRoot : accountsList) {
                    Log.d("MandarinLog", "account: "+ accountRoot.getUserId());
                    Map<String, Object> objectMap = new HashMap<String, Object>();
                    objectMap.put(ATTRIBUTE_NAME_TEXT, accountRoot.getUserNick());
                    objectMap.put(ATTRIBUTE_NAME_IMAGE, accountRoot.getServiceIcon());
                    data.add(objectMap);
                }
                // массив имен атрибутов, из которых будут читаться данные
                String[] from = {ATTRIBUTE_NAME_TEXT, ATTRIBUTE_NAME_IMAGE};
                // массив ID View-компонентов, в которые будут вставлять данные
                int[] to = {R.id.accountNickName, R.id.accountAvatar};

                SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.account_item, from, to);

                // Bind to our new adapter.
                listView.setAdapter(sAdapter);
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
