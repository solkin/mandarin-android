package com.tomclaw.mandarin.main;

import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.DataProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.ChatPagerAdapter;
import com.viewpageindicator.PageIndicator;
import com.viewpageindicator.TitlePageIndicator;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
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
            case R.id.close_chat_menu: {
                int position = mPager.getCurrentItem();
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataProvider.ROSTER_BUDDY_DIALOG, 0);
                getContentResolver().update(Settings.BUDDY_RESOLVER_URI, contentValues,
                        DataProvider.ROW_AUTO_ID + "='" + mAdapter.getPageBuddyDbId(position) + "'", null);
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
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        /** View pager **/
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mIndicator.notifyDataSetChanged();
            }
        };
        mIndicator = (TitlePageIndicator) findViewById(R.id.chat_indicator);

        mAdapter = new ChatPagerAdapter(this, getSupportLoaderManager(), mIndicator);
        mPager = (ViewPager) findViewById(R.id.chat_pager);
        mPager.setAdapter(mAdapter);

        mIndicator.setViewPager(mPager);
        mIndicator.setCurrentItem(0);
    }

    @Override
    public void onCoreServiceDown() {

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }
}
