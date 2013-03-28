package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.widget.SearchView;
import com.tomclaw.mandarin.R;
import com.viewpageindicator.TitlePageIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class AccountsActivity extends ChiefActivity implements
        ActionBar.OnNavigationListener  {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.accounts_list_menu, menu);
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        setContentView(R.layout.accounts_list);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Initialize accounts list
        initAccountsList();
    }

    private void initAccountsList() {
        ListView listView = (ListView) findViewById(R.id.listView);

        final String ATTRIBUTE_NAME_TEXT = "text";
        final String ATTRIBUTE_NAME_IMAGE = "image";
        ArrayList<Map<String, Object>> data;
        Map<String, Object> m;
        // упаковываем данные в понятную для адаптера структуру
        data = new ArrayList<Map<String, Object>>();
        for (int i = 1; i < 5; i++) {
            m = new HashMap<String, Object>();
            m.put(ATTRIBUTE_NAME_TEXT, "sometext " + i);
            m.put(ATTRIBUTE_NAME_IMAGE, R.drawable.ic_launcher);
            data.add(m);
        }
        // массив имен атрибутов, из которых будут читаться данные
        String[] from = { ATTRIBUTE_NAME_TEXT, ATTRIBUTE_NAME_IMAGE };
        // массив ID View-компонентов, в которые будут вставлять данные
        int[] to = { R.id.accountNickName, R.id.accountAvatar };

        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.account_item, from, to);

        // Bind to our new adapter.
        listView.setAdapter(sAdapter);
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
