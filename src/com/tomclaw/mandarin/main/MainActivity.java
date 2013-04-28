package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RosterProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.viewpageindicator.PageIndicator;
import com.viewpageindicator.TitlePageIndicator;

import java.util.List;
import java.util.Vector;

public class MainActivity extends ChiefActivity implements
        ActionBar.OnNavigationListener {

    CustomPagerAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // super.onSaveInstanceState(outState);
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
            case android.R.id.home:
                Intent intent = new Intent(this, AccountsActivity.class);
                startActivity(intent);
                return true;
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
        ExpandableListView listView1 = new ExpandableListView(this);
        ExpandableListView listView2 = new ExpandableListView(this);
        ExpandableListView listView3 = new ExpandableListView(this);

        Vector<View> pages = new Vector<View>();

        pages.add(listView1);
        pages.add(listView2);
        pages.add(listView3);

        /************** Example of BuddyItem list ******************/
        try {
            List<AccountRoot> accountRoots = getServiceInteraction().getAccountsList();

            listView1.setAdapter(new BuddyAdapter(this, accountRoots));
            listView2.setAdapter(new BuddyAdapter(this, accountRoots));


            Cursor cursor = getContentResolver().query(Settings.GROUP_RESOLVER_URI, null, null,
                    null, null);
            startManagingCursor(cursor);


            String groupFrom[] = {RosterProvider.ROSTER_GROUP_NAME};
            int groupTo[] = { R.id.groupName };

            String from[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK };
            int to[] = { R.id.buddyId, R.id.buddyNick };
            SimpleCursorTreeAdapter adapter = new SimpleCursorTreeAdapter(this,
                    cursor, R.layout.group_item, R.layout.group_item,
                    groupFrom, groupTo, R.layout.buddy_item, R.layout.buddy_item, from, to) {

                @Override
                protected Cursor getChildrenCursor(Cursor groupCursor) {
                    Log.d(Settings.LOG_TAG, "getChildrenCursor");
                    int columnIndex = groupCursor.getColumnIndex(RosterProvider.ROSTER_GROUP_NAME);
                    String groupName = groupCursor.getString(columnIndex);
                    return getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_GROUP + "='" + groupName +"'",
                            null, null);
                }
            };

            listView3.setAdapter(adapter);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        /** View pager **/
        mAdapter = new CustomPagerAdapter(this, pages);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setCurrentItem(2);
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

    class CustomPagerAdapter extends PagerAdapter {
        private Context mContext;
        private Vector<View> pages;

        public CustomPagerAdapter(Context context, Vector<View> pages) {
            this.mContext = context;
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
