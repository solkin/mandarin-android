package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.adapters.RosterFavoriteAdapter;
import com.tomclaw.mandarin.main.adapters.RosterGeneralAdapter;
import com.tomclaw.mandarin.main.adapters.RosterOnlineAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ChiefActivity implements ActionBar.OnNavigationListener {

    private RosterPagerAdapter mAdapter;
    private ViewPager mPager;
    private PagerSlidingTabStrip mIndicator;
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

    @Override
    public void onCoreServiceReady() {
        Log.d(Settings.LOG_TAG, "onCoreServiceReady");
        setContentView(R.layout.buddy_list);
        ActionBar bar = getSupportActionBar();
        // bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        // bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        /** Status spinner **/
        /*ArrayAdapter<CharSequence> listAdapter = ArrayAdapter.createFromResource(this, R.array.status_list,
                R.layout.sherlock_spinner_item);
        listAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        bar.setListNavigationCallbacks(listAdapter, this);/
        /** Lists **/
        pages.clear();
        // Favorite.
        final ListView favoriteList = new ListView(this);
        final RosterFavoriteAdapter favoriteAdapter = new RosterFavoriteAdapter(this, getSupportLoaderManager());
        favoriteList.setAdapter(favoriteAdapter);
        favoriteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                int buddyDbId = favoriteAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Check out dialog with buddy (db id): " + buddyDbId);
                intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);*/

                int buddyDbId = favoriteAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Opening dialog with buddy (db id): " + buddyDbId);
                try {
                    // Trying to open dialog with this buddy.
                    QueryHelper.modifyDialog(getContentResolver(), buddyDbId, true);
                    // Open chat dialog for this buddy.
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                    startActivity(intent);
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
            }
        });
        pages.add(favoriteList);
        // Dialogs.
        final ListView dialogsList = new ListView(this);
        final RosterDialogsAdapter dialogsAdapter = new RosterDialogsAdapter(this, getSupportLoaderManager());
        dialogsList.setAdapter(dialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                int buddyDbId = dialogsAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Check out dialog with buddy (db id): " + buddyDbId);
                intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);
            }
        });
        pages.add(dialogsList);
        // Online.
        ListView onlineList = new ListView(this);
        final RosterOnlineAdapter onlineAdapter = new RosterOnlineAdapter(this, getSupportLoaderManager());
        onlineList.setAdapter(onlineAdapter);
        onlineList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int buddyDbId = onlineAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Opening dialog with buddy (db id): " + buddyDbId);
                try {
                    // Trying to open dialog with this buddy.
                    QueryHelper.modifyDialog(getContentResolver(), buddyDbId, true);
                    // Open chat dialog for this buddy.
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                    startActivity(intent);
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
            }
        });
        pages.add(onlineList);
        // All friends.
        ExpandableListView generalList = new ExpandableListView(this);
        final RosterGeneralAdapter generalAdapter = new RosterGeneralAdapter(this, getSupportLoaderManager());
        generalList.setAdapter(generalAdapter);
        generalList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                int buddyDbId = generalAdapter.getBuddyDbId(groupPosition, childPosition);
                Log.d(Settings.LOG_TAG, "Opening dialog with buddy (db id): " + buddyDbId);
                try {
                    // Trying to open dialog with this buddy.
                    QueryHelper.modifyDialog(getContentResolver(), buddyDbId, true);
                    // Open chat dialog for this buddy.
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                    startActivity(intent);
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
                return true;
            }
        });
        pages.add(generalList);
        /** View pager **/
        mAdapter = new RosterPagerAdapter(pages);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (PagerSlidingTabStrip) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setIndicatorColorResource(R.color.background_action_bar);
        mPager.setCurrentItem(2);
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
