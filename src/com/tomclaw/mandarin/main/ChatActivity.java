package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.ChatDialogsAdapter;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private ActionBarHelper actionBarHelper;
    private ChatDialogsAdapter chatDialogsAdapter;
    private ListView chatList;
    private HistorySelection historySelection;
    private ChatHistoryAdapter chatHistoryAdapter;
    private ActionMode mActionMode;
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
            mActionMode = mode;
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            String selection = historySelection.buildSelection();
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
                    break;
                case R.id.message_create_note:
                    break;
                case R.id.message_share:
                    break;
                default:
                    return false;
            }
            mode.finish();
            return true;
        }

        // Called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            if(historySelection.getSelectionMode()) {
                historySelection.finish();
                chatHistoryAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        historySelection.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                drawerToggle.onOptionsItemSelected(item);
                return true;
            }
            case R.id.close_chat_menu: {
                try {
                    QueryHelper.modifyDialog(getContentResolver(), chatHistoryAdapter.getBuddyDbId(), false);
                } catch (Exception ignored) {
                    // Nothing to do in this case.
                }
                return true;
            }
            case R.id.clear_history_menu: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.clear_history_title);
                builder.setMessage(R.string.clear_history_text);
                builder.setPositiveButton(R.string.yes_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            QueryHelper.clearHistory(getContentResolver(), chatHistoryAdapter.getBuddyDbId());
                        } catch (Exception ignored) {
                            // Nothing to do in this case.
                        }
                    }
                });
                builder.setNegativeButton(R.string.do_not_clear, null);
                builder.show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCoreServiceReady() {
        Bundle bundle = getIntent().getExtras();

        int buddyDbId = -1;
        // Checking for bundle condition.
        if (bundle != null && bundle.containsKey(GlobalProvider.HISTORY_BUDDY_DB_ID)) {
            // Setup active page.
            buddyDbId = bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, 0);
        }

        Log.d(Settings.LOG_TAG, "onCoreServiceReady");
        setContentView(R.layout.chat_activity);
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        historySelection = new HistorySelection();
        chatList = (ListView) findViewById(R.id.chat_list);
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this,
                getLoaderManager(), historySelection, buddyDbId);
        chatList.setAdapter(chatHistoryAdapter);
        // Long-click listener to activate action mode and show check-boxes.
        AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Checking for action mode is already activated.
                if (historySelection.getSelectionMode()) {
                    // Hm. Action mode is already active.
                    return false;
                }
                // Update selection data.
                historySelection.setSelectionMode(true);
                // Update history adapter to show checkboxes.
                // historySelection.notifyHistoryAdapter();
                chatHistoryAdapter.notifyDataSetChanged();
                // Starting action mode.
                startActionMode(mActionModeCallback);
                return true;
            }
        };
        chatList.setOnItemLongClickListener(itemLongClickListener);
        // Click listener for item clicked events (in selection mode).
        AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Checking for action mode is activated.
                if (historySelection.getSelectionMode()) {
                    boolean selectionExist = historySelection.isSelectionExist(position);
                    historySelection.setSelection(position, selectionExist ?
                            null : chatHistoryAdapter.getItemText(position));
                    chatHistoryAdapter.notifyDataSetChanged();
                }
            }
        };
        chatList.setOnItemClickListener(itemClickListener);

        chatDialogsAdapter = new ChatDialogsAdapter(this, getLoaderManager());

        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(chatDialogsAdapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        drawerList.setCacheColorHint(0);
        drawerList.setScrollingCacheEnabled(false);
        drawerList.setScrollContainer(false);
        drawerList.setFastScrollEnabled(true);
        drawerList.setSmoothScrollbarEnabled(true);

        actionBarHelper = new ActionBarHelper();
        actionBarHelper.init();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // ActionBarDrawerToggle provides convenient helpers for tying together the
        // prescribed interactions between a top-level sliding drawer and the action bar.
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer_dark,
                R.string.drawer_open, R.string.drawer_close);
        drawerToggle.syncState();

        // Send button and message field initialization.
        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        final TextView messageText = (TextView) findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                    String cookie = String.valueOf(System.currentTimeMillis());
                    String appSession = getServiceInteraction().getAppSession();
                    String message = messageText.getText().toString();
                    QueryHelper.insertMessage(getContentResolver(), appSession,
                            buddyDbId, 2, // TODO: real message type
                            cookie, message, false);
                    // Sending protocol message request.
                    RequestHelper.requestMessage(getContentResolver(), appSession,
                            buddyDbId, cookie, message);
                    // Clearing text view.
                    messageText.setText("");
                } catch (Exception e) {
                    // TODO: Couldn't put message into database. This exception must be processed.
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * This list item click listener implements very simple view switching by
     * changing the primary content text. The drawer is closed when a selection
     * is made.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Checking for history selection now and action mode is not null, finish it!
            if(historySelection.getSelectionMode() && mActionMode != null) {
                // Finish history selection first to only close action mode on finish method bottom.
                historySelection.finish();
                mActionMode.finish();
            }
            // Changing chat history adapter loader.
            int buddyDbId = chatDialogsAdapter.getBuddyDbId(position);
            chatHistoryAdapter.setBuddyDbId(buddyDbId);
            actionBarHelper.setTitle(chatDialogsAdapter.getBuddyNick(position));
            drawerLayout.closeDrawer(drawerList);
        }
    }

    private class ActionBarHelper {
        private final ActionBar mActionBar;
        private CharSequence mDrawerTitle;
        private CharSequence mTitle;

        private ActionBarHelper() {
            mActionBar = getActionBar();
        }

        public void init() {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
            mTitle = mDrawerTitle = getTitle();
        }

        /**
         * When the drawer is closed we restore the action bar state reflecting
         * the specific contents in view.
         */
        public void onDrawerClosed() {
            mActionBar.setTitle(mTitle);
        }

        /**
         * When the drawer is open we set the action bar to a generic title. The
         * action bar should only contain data relevant at the top level of the
         * nav hierarchy represented by the drawer, as the rest of your content
         * will be dimmed down and non-interactive.
         */
        public void onDrawerOpened() {
            mActionBar.setTitle(mDrawerTitle);
        }

        public void setTitle(CharSequence title) {
            mTitle = title;
        }
    }
}
