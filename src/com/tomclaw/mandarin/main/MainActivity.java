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
import android.widget.*;
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
        ListView listView1 = new ListView(this);
        ExpandableListView listView2 = new ExpandableListView(this);
        ExpandableListView listView3 = new ExpandableListView(this);

        Vector<View> pages = new Vector<View>();

        pages.add(listView1);
        pages.add(listView2);
        pages.add(listView3);

        /************** Example of BuddyItem list ******************/
        try {
            List<AccountRoot> accountRoots = getServiceInteraction().getAccountsList();

            /********* Dialogs *********/
            Cursor cursor0 = getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null,
                    RosterProvider.ROSTER_BUDDY_DIALOG + "='" + 1 +"'",
                    null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
            startManagingCursor(cursor0);


            String groupFrom0[] = {RosterProvider.ROSTER_GROUP_NAME};
            int groupTo0[] = { R.id.groupName };

            String from0[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS };
            int to0[] = { R.id.buddyId, R.id.buddyNick, R.id.buddyStatus };
            SimpleCursorAdapter adapter0 = new SimpleCursorAdapter(this, R.layout.buddy_item,
                    cursor0, from0, to0, 0) {
            };
            listView1.setAdapter(adapter0);

            /********* Online *********/
            Cursor cursor = getContentResolver().query(Settings.GROUP_RESOLVER_URI, null, null,
                    null, RosterProvider.ROSTER_GROUP_NAME + " ASC");
            startManagingCursor(cursor);


            String groupFrom[] = {RosterProvider.ROSTER_GROUP_NAME};
            int groupTo[] = { R.id.groupName };

            String from[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS };
            int to[] = { R.id.buddyId, R.id.buddyNick, R.id.buddyStatus };
            SimpleCursorTreeAdapter adapter = new SimpleCursorTreeAdapter(this,
                    cursor, R.layout.group_item, R.layout.group_item,
                    groupFrom, groupTo, R.layout.buddy_item, R.layout.buddy_item, from, to) {

                @Override
                protected Cursor getChildrenCursor(Cursor groupCursor) {
                    Log.d(Settings.LOG_TAG, "getChildrenCursor");
                    int columnIndex = groupCursor.getColumnIndex(RosterProvider.ROSTER_GROUP_NAME);
                    String groupName = groupCursor.getString(columnIndex);
                    return getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_GROUP + "='" + groupName +"'" + " AND "
                            + RosterProvider.ROSTER_BUDDY_STATE + "='" + 1 + "'",
                            null, RosterProvider.ROSTER_BUDDY_NICK + " ASC");
                }
            };
            listView2.setAdapter(adapter);

            /********* All friends *********/
            Cursor cursor1 = getContentResolver().query(Settings.GROUP_RESOLVER_URI, null, null,
                    null, RosterProvider.ROSTER_GROUP_NAME + " ASC");
            startManagingCursor(cursor1);


            String groupFrom1[] = {RosterProvider.ROSTER_GROUP_NAME};
            int groupTo1[] = { R.id.groupName };

            String from1[] = { RosterProvider.ROSTER_BUDDY_ID, RosterProvider.ROSTER_BUDDY_NICK, RosterProvider.ROSTER_BUDDY_STATUS };
            int to1[] = { R.id.buddyId, R.id.buddyNick, R.id.buddyStatus };
            SimpleCursorTreeAdapter adapter1 = new SimpleCursorTreeAdapter(this,
                    cursor1, R.layout.group_item, R.layout.group_item,
                    groupFrom1, groupTo1, R.layout.buddy_item, R.layout.buddy_item, from1, to1) {

                @Override
                protected Cursor getChildrenCursor(Cursor groupCursor) {
                    Log.d(Settings.LOG_TAG, "getChildrenCursor");
                    int columnIndex = groupCursor.getColumnIndex(RosterProvider.ROSTER_GROUP_NAME);
                    String groupName = groupCursor.getString(columnIndex);
                    return getContentResolver().query(Settings.BUDDY_RESOLVER_URI, null, RosterProvider.ROSTER_BUDDY_GROUP + "='" + groupName +"'",
                            null, RosterProvider.ROSTER_BUDDY_STATE + " DESC," + RosterProvider.ROSTER_BUDDY_NICK + " ASC");
                }
            };

            listView3.setAdapter(adapter1);
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
