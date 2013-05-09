package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.QueryHelper;
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

    public static final String DIALOG_ID = "dialog_id";
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
                try {
                    QueryHelper.closeDialog(getContentResolver(), getCurrentPageBuddyDbId());
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
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
        if (getIntent().getExtras() != null){
            mIndicator.setCurrentItem(getIntent().getExtras().getInt(DIALOG_ID, 0));
        }
        else {
            mIndicator.setCurrentItem(0);
        }
        /** Send button **/
        ImageButton sendButton = (ImageButton)findViewById(R.id.send_button);
        final TextView messageText = (TextView) findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    QueryHelper.insertMessage(getContentResolver(), getCurrentPageBuddyDbId(), 1,
                            String.valueOf(System.currentTimeMillis()), messageText.getText().toString());
                } catch (Exception e) {
                    // Couldn't pul message into database. This exception must be processed.
                }
            }
        });
    }

    @Override
    public void onCoreServiceDown() {

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }

    /**
     * Obtain current item position and checking for it valid.
     * @return
     * @throws Exception
     */
    private int getCurrentPageBuddyDbId() throws Exception {
        int position = mPager.getCurrentItem();
        if(position < 0) {
            throw new Exception("No active page.");
        }
        return mAdapter.getPageBuddyDbId(position);
    }
}
