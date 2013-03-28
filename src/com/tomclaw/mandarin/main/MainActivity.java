package com.tomclaw.mandarin.main;

import com.actionbarsherlock.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import com.actionbarsherlock.widget.SearchView;
import android.widget.SpinnerAdapter;
import com.actionbarsherlock.view.Menu;
import com.tomclaw.mandarin.R;
import com.viewpageindicator.PageIndicator;
import com.viewpageindicator.TitlePageIndicator;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends ChiefActivity implements
        ActionBar.OnNavigationListener {

    GroupFragmentAdapter mAdapter;
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
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onCoreServiceReady() {
        Log.d(LOG_TAG, "onCoreServiceReady");
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
        /** View pager **/
        mAdapter = new GroupFragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setCurrentItem(2);
    }

    public void onCoreServiceIntent(Intent intent) {
        Log.d(LOG_TAG, "onCoreServiceIntent");
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.d(LOG_TAG, "selected: position = " + itemPosition + ", id = "
                + itemId);
        try {
            Log.d(LOG_TAG, "service up time = " + getServiceInteraction().getUpTime());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    class GroupFragmentAdapter extends FragmentPagerAdapter {
        protected final String[] GROUPS = getResources().getStringArray(R.array.default_groups);

        public GroupFragmentAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return new GroupFragment();
        }

        @Override
        public int getCount() {
            return GROUPS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return GROUPS[position % getCount()];
        }
    }
}
