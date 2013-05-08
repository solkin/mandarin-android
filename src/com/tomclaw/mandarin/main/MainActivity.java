package com.tomclaw.mandarin.main;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DataProvider;
import com.tomclaw.mandarin.core.DatabaseHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.adapters.RosterGeneralAdapter;
import com.tomclaw.mandarin.main.adapters.RosterOnlineAdapter;
import com.viewpageindicator.PageIndicator;
import com.viewpageindicator.TitlePageIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends ChiefActivity implements ActionBar.OnNavigationListener {

    private RosterPagerAdapter mAdapter;
    private ViewPager mPager;
    private PageIndicator mIndicator;
    private List<View> pages = new ArrayList<View>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.buddy_list_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Configure the search info and add any event listeners
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent intent = new Intent(this, AccountsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.show_dialogs: {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Button UI handling
     *
     * @param v
     */
    public void onClickClose(View v) {
        stopCoreService();
    }

    /**
     * Button UI handling
     *
     * @param v
     */
    public void onClickIntent(View v) {
        try {
            getServiceInteraction().initService();
        } catch (RemoteException e) {
            Log.e(Settings.LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onCoreServiceReady() {
        Log.d(Settings.LOG_TAG, "onCoreServiceReady");
        setContentView(R.layout.buddy_list);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        /** Status spinner **/
        ArrayAdapter<CharSequence> listAdapter = ArrayAdapter.createFromResource(this, R.array.status_list,
                R.layout.sherlock_spinner_item);
        listAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        bar.setListNavigationCallbacks(listAdapter, this);
        /** Lists **/
        pages.clear();
        /********* Dialogs *********/
        ListView dialogsList = new ListView(this);
        RosterDialogsAdapter dialogsAdapter = new RosterDialogsAdapter(this, getSupportLoaderManager());
        dialogsList.setAdapter(dialogsAdapter);
        pages.add(dialogsList);
        /********* Online *********/
        ListView onlineList = new ListView(this);
        RosterOnlineAdapter onlineAdapter = new RosterOnlineAdapter(this, getSupportLoaderManager());
        onlineList.setAdapter(onlineAdapter);
        pages.add(onlineList);
        /********* All friends *********/
        ExpandableListView generalList = new ExpandableListView(this);
        RosterGeneralAdapter generalAdapter = new RosterGeneralAdapter(this, getSupportLoaderManager());
        generalList.setAdapter(generalAdapter);
        pages.add(generalList);
        /** View pager **/
        mAdapter = new RosterPagerAdapter(pages);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setCurrentItem(2);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Random random = new Random(System.currentTimeMillis());
                Cursor cursor = getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null,
                        DataProvider.ROSTER_BUDDY_ID + "='" + "burova@molecus.com" + "'", null, null);
                if(cursor.getCount() == 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DataProvider.ROSTER_BUDDY_ID, "burova@molecus.com");
                    cv.put(DataProvider.ROSTER_BUDDY_NICK, "Burova");
                    cv.put(DataProvider.ROSTER_BUDDY_GROUP, "Friends");
                    cv.put(DataProvider.ROSTER_BUDDY_STATUS, R.drawable.status_icq_online);
                    cv.put(DataProvider.ROSTER_BUDDY_STATE, 1);
                    cv.put(DataProvider.ROSTER_BUDDY_DIALOG, 1);
                    Uri newUri = getContentResolver().insert(Settings.BUDDY_RESOLVER_URI, cv);
                    Log.d(Settings.LOG_TAG, "insert, result Uri : " + newUri.toString());
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataProvider.ROSTER_GROUP_NAME, "Friends");
                    Uri uri = getContentResolver().insert(Settings.GROUP_RESOLVER_URI, contentValues);
                    Log.d(Settings.LOG_TAG, "insert, result Uri : " + uri.toString());
                    cursor = getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null,
                            DataProvider.ROSTER_BUDDY_ID + "='" + "burova@molecus.com" + "'", null, null);
                }


                cursor.moveToPosition(0);
                long id = cursor.getLong(cursor.getColumnIndex(DataProvider.ROW_AUTO_ID));
                for(int c=0; c<30; c++) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    ContentValues cv2 = new ContentValues();
                    cv2.put(DataProvider.HISTORY_BUDDY_DB_ID, String.valueOf(id));
                    cv2.put(DataProvider.HISTORY_BUDDY_NICK, "Burova");
                    cv2.put(DataProvider.HISTORY_MESSAGE_TYPE, "1");
                    cv2.put(DataProvider.HISTORY_MESSAGE_COOKIE, String.valueOf(System.currentTimeMillis()));
                    cv2.put(DataProvider.HISTORY_MESSAGE_STATE, "1");
                    cv2.put(DataProvider.HISTORY_MESSAGE_TIME, System.currentTimeMillis());
                    cv2.put(DataProvider.HISTORY_MESSAGE_TEXT, DatabaseHelper.generateRandomText(random));
                    getContentResolver().insert(Settings.HISTORY_RESOLVER_URI, cv2);
                }
            }
        };
        thread.start();
    }

    @Override
    public void onCoreServiceDown() {
        Log.d(Settings.LOG_TAG, "onCoreServiceDown");
    }

    public void onCoreServiceIntent(Intent intent) {
        Log.d(Settings.LOG_TAG, "onCoreServiceIntent");
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.d(Settings.LOG_TAG, "selected: position = " + itemPosition + ", id = "
                + itemId);
        return false;
    }

    private class RosterPagerAdapter extends PagerAdapter {
        private List<View> pages;

        public RosterPagerAdapter(List<View> pages) {
            this.pages = pages;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View page = pages.get(position);
            container.addView(page);
            return page;
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.default_groups)[position % getCount()];
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
