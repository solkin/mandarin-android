package com.tomclaw.mandarin.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.ChatPagerAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private ChatPagerAdapter mAdapter;
    private ViewPager mPager;
    private PagerSlidingTabStrip mIndicator;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.chat_history_edit_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            String selection = HistorySelection.getInstance().complete();
            switch (item.getItemId()) {
                case R.id.message_copy:
                    if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                                getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(selection);
                    } else {
                        ClipboardManager clipboardManager = (ClipboardManager)
                                getSystemService(CLIPBOARD_SERVICE);
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("", selection));
                    }
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                case R.id.message_create_note:
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                case R.id.message_share:
                    // Action picked, so close the CAB
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            HistorySelection.getInstance().setSelectionMode(false);
            mAdapter.notifyDataSetChanged();
        }
    };

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
                    QueryHelper.modifyDialog(getContentResolver(), getCurrentPageBuddyDbId(), false);
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
        Runnable onUpdate = new Runnable() {
            @Override
            public void run() {
                Bundle bundle = getIntent().getExtras();
                // Checking for bundle condition.
                if (bundle != null && bundle.containsKey(GlobalProvider.HISTORY_BUDDY_DB_ID)) {
                    // Setup active page.
                    int position = mAdapter.getPagePosition(bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, 0));
                    mPager.setCurrentItem(position);
                    getIntent().removeExtra(GlobalProvider.HISTORY_BUDDY_DB_ID);
                }
                // Notify page indicator and base adapter data was changed.
                mIndicator.notifyDataSetChanged();
                mAdapter.notifyDataSetChanged();
            }
        };
        Runnable onLongClick = new Runnable() {
            @Override
            public void run() {
                // Checking for action mode is already activated.
                if (HistorySelection.getInstance().getSelectionMode()) {
                    return;
                }
                // Start the CAB using the ActionMode.Callback defined above
                HistorySelection.getInstance().setSelectionMode(true);
                startActionMode(mActionModeCallback);
            }
        };
        mIndicator = (PagerSlidingTabStrip) findViewById(R.id.chat_indicator);
        mAdapter = new ChatPagerAdapter(this, getSupportLoaderManager(), onUpdate, onLongClick);
        mPager = (ViewPager) findViewById(R.id.chat_pager);
        mPager.setAdapter(mAdapter);
        mIndicator.setViewPager(mPager);
        mIndicator.setIndicatorColorResource(R.color.background_action_bar);
        /** Send button **/
        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        final TextView messageText = (TextView) findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String cookie = String.valueOf(System.currentTimeMillis());
                    String appSession = getServiceInteraction().getAppSession();
                    int accountDbId = getCurrentPageAccountDbId();
                    int buddyDbId = getCurrentPageBuddyDbId();
                    String message = messageText.getText().toString();
                    QueryHelper.insertMessage(getContentResolver(), appSession,
                            accountDbId, buddyDbId, 2, // TODO: real message type
                            cookie, message, false);
                    // Sending protocol message request.
                    RequestHelper.requestMessage(getContentResolver(), appSession,
                            accountDbId, buddyDbId, cookie, message);
                    // Clearing text view.
                    messageText.setText("");
                } catch (Exception e) {
                    // Couldn't put message into database. This exception must be processed.
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
     *
     * @return int
     * @throws Exception
     */
    private int getCurrentPageBuddyDbId() throws Exception {
        int position = mPager.getCurrentItem();
        if (position < 0) {
            throw new Exception("No active page.");
        }
        return mAdapter.getPageBuddyDbId(position);
    }

    public int getCurrentPageAccountDbId() throws Exception {
        int position = mPager.getCurrentItem();
        if (position < 0) {
            throw new Exception("No active page.");
        }
        return mAdapter.getPageAccountDbId(position);
    }
}
