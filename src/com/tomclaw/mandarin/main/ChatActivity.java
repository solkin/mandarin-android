package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.viewpageindicator.PageIndicator;
import com.viewpageindicator.TitlePageIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatActivity extends ChiefActivity {

    private ChatPagerAdapter mAdapter;
    private ViewPager mPager;
    private PageIndicator mIndicator;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent intent = new Intent(this, MainActivity.class);
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
        setContentView(R.layout.chat_pager);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.dialogs);
        /** View pager **/
        List<View> pages = new ArrayList<View>();
        View view = getLayoutInflater().inflate(R.layout.chat_dialog, null);
        pages.add(view);
        View view1 = getLayoutInflater().inflate(R.layout.chat_dialog, null);
        pages.add(view1);
        View view2 = getLayoutInflater().inflate(R.layout.chat_dialog, null);
        pages.add(view2);
        mAdapter = new ChatPagerAdapter(pages);
        mPager = (ViewPager) findViewById(R.id.chat_pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator) findViewById(R.id.chat_indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setCurrentItem(0);
    }

    @Override
    public void onCoreServiceDown() {

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }

    private class ChatPagerAdapter extends PagerAdapter {
        private List<View> pages;

        public ChatPagerAdapter(List<View> pages) {
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
