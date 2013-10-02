package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.adapters.ChatDialogsAdapter;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.util.Collection;

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
    private CharSequence title;

    private ChatListView chatList;

    private ChatDialogsAdapter chatDialogsAdapter;
    private ChatHistoryAdapter chatHistoryAdapter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(Settings.LOG_TAG, "onNewIntent");

        int buddyDbId = getIntentBuddyDbId(intent);

        setTitleByBuddyDbId(buddyDbId);

        chatHistoryAdapter.setBuddyDbId(buddyDbId);
    }

    @Override
    public void onCoreServiceReady() {
        Log.d(Settings.LOG_TAG, "onCoreServiceReady");

        setContentView(R.layout.chat_activity);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        int buddyDbId = getIntentBuddyDbId(getIntent());

        setTitleByBuddyDbId(buddyDbId);

        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, getLoaderManager(), buddyDbId);

        chatList = (ChatListView) findViewById(R.id.chat_list);
        chatList.setAdapter(chatHistoryAdapter);
        chatList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        chatList.setOnScrollListener(new ChatScrollListener());
        chatList.setOnDataChangedListener(new ChatListView.DataChangedListener() {
            @Override
            public void onDataChanged() {
                readVisibleMessages();
            }
        });

        chatDialogsAdapter = new ChatDialogsAdapter(this, getLoaderManager());
        chatDialogsAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                // Checking for selection is invalid.
                if(chatDialogsAdapter.getBuddyPosition(chatHistoryAdapter.getBuddyDbId()) == -1) {
                    Log.d(Settings.LOG_TAG, "No selected item anymore.");
                    // Checking for another opened chat.
                    if(chatDialogsAdapter.getCount() > 0) {
                        int moreActiveBuddyPosition;
                        try {
                            // Trying to obtain more active dialog position.
                            moreActiveBuddyPosition = chatDialogsAdapter.getBuddyPosition(
                                    QueryHelper.getMoreActiveDialog(getContentResolver()));
                        } catch (BuddyNotFoundException ignored) {
                            // Something really strange. No opened dialogs. So switch to first position.
                            moreActiveBuddyPosition = 0;
                        } catch (MessageNotFoundException ignored) {
                            // No messages in all opened dialogs. So switch to first position.
                            moreActiveBuddyPosition = 0;
                        }
                        selectItem(moreActiveBuddyPosition);
                    } else {
                        // No more content on this activity.
                        finish();
                    }
                }
            }
        });

        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(chatDialogsAdapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer_dark,
                R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.dialogs);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                if(newState == DrawerLayout.STATE_SETTLING
                        || newState == DrawerLayout.STATE_DRAGGING) {
                    int position = chatDialogsAdapter.getBuddyPosition(chatHistoryAdapter.getBuddyDbId());
                    drawerList.setItemChecked(position, true);
                }
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Send button and message field initialization.
        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        final TextView messageText = (TextView) findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    try {
                        int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                        String cookie = String.valueOf(System.currentTimeMillis());
                        String appSession = getServiceInteraction().getAppSession();
                        QueryHelper.insertMessage(getContentResolver(), buddyDbId, 2, // TODO: real message type
                                cookie, message, false);
                        // Sending protocol message request.
                        RequestHelper.requestMessage(getContentResolver(), appSession,
                                buddyDbId, cookie, message);
                        // Clearing text view.
                        messageText.setText("");
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO: Couldn't put message into database. This exception must be processed.
                    }
                }
            }
        });
    }

    @Override
    public void onCoreServiceDown() {
        // TODO: must be implemented.
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // TODO: must be implemented.
    }

    private int getIntentBuddyDbId(Intent intent) {
        Bundle bundle = intent.getExtras();

        int buddyDbId = -1;
        // Checking for bundle condition.
        if (bundle != null && bundle.containsKey(GlobalProvider.HISTORY_BUDDY_DB_ID)) {
            // Setup active page.
            buddyDbId = bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, 0);
        }

        return buddyDbId;
    }

    private void setTitleByBuddyDbId(int buddyDbId) {
        try {
            // This will provide buddy nick by db id.
            setTitle(QueryHelper.getBuddyNick(getContentResolver(), buddyDbId));
        } catch (BuddyNotFoundException ignored) {
            Log.d(Settings.LOG_TAG, "No buddy fount by specified buddyDbId");
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            // Pass any configuration change to the drawer toggles
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    /**
     * This list item click listener implements very simple view switching by
     * changing the primary content text. The drawer is closed when a selection
     * is made.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // Changing chat history adapter loader.
        int buddyDbId = chatDialogsAdapter.getBuddyDbId(position);
        chatHistoryAdapter.setBuddyDbId(buddyDbId);
        setTitle(chatDialogsAdapter.getBuddyNick(position));
        drawerLayout.closeDrawer(drawerList);
    }

    private boolean readVisibleMessages() {
        int firstVisiblePosition = chatList.getFirstVisiblePosition();
        int lastVisiblePosition = chatList.getLastVisiblePosition();
        Log.d(Settings.LOG_TAG, "Reading visible messages ["
                + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
        // Checking for the list view is ready.
        if(lastVisiblePosition >= firstVisiblePosition) {
            try {
                QueryHelper.readMessages(getContentResolver(),
                        chatHistoryAdapter.getBuddyDbId(),
                        chatHistoryAdapter.getMessageDbId(firstVisiblePosition),
                        chatHistoryAdapter.getMessageDbId(lastVisiblePosition));
                return true;
            } catch (MessageNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while marking messages as read positions ["
                        + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
            }
        }
        return false;
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper();
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.chat_history_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.message_copy:
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", getSelectedMessages()));
                    break;
                case R.id.message_share:
                    startActivity(createShareIntent());
                    break;
                case R.id.message_create_note:
                    break;
                default:
                    return false;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }

        private String getSelectedMessages() {
            StringBuilder selectionBuilder = new StringBuilder();
            // Obtain selected positions.
            Collection<Integer> selectedPositions = selectionHelper.getSelectedPositions();
            // Iterating for all selected positions.
            for (int position : selectedPositions) {
                try {
                    selectionBuilder.append(chatHistoryAdapter.getMessageText(position)).append('\n').append('\n');
                } catch (MessageNotFoundException ignored) {
                    Log.d(Settings.LOG_TAG, "Error while copying message on position " + position);
                }
            }
            return selectionBuilder.toString().trim();
        }

        private Intent createShareIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getSelectedMessages());
            return shareIntent;
        }
    }

    private class ChatScrollListener implements AbsListView.OnScrollListener {

        private int startFirstVisiblePosition, startLastVisiblePosition;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            int firstVisiblePosition = view.getFirstVisiblePosition();
            int lastVisiblePosition = view.getLastVisiblePosition();
            switch (scrollState) {
                case SCROLL_STATE_TOUCH_SCROLL: {
                    // Scroll stared.
                    startFirstVisiblePosition = firstVisiblePosition;
                    startLastVisiblePosition = lastVisiblePosition;
                    break;
                }
                case SCROLL_STATE_IDLE: {
                    // Scroll ended.
                    int firstPosition;
                    int lastPosition;
                    if(firstVisiblePosition > startFirstVisiblePosition) {
                        // Scroll to bottom.
                        firstPosition = startFirstVisiblePosition;
                        lastPosition = lastVisiblePosition;
                    } else {
                        // Scroll to top.
                        firstPosition = firstVisiblePosition;
                        lastPosition = startLastVisiblePosition;
                    }
                    Log.d(Settings.LOG_TAG, "Scroll: " + firstPosition + " -> " + lastPosition);
                    try {
                        QueryHelper.readMessages(getContentResolver(),
                                chatHistoryAdapter.getBuddyDbId(),
                                chatHistoryAdapter.getMessageDbId(firstPosition),
                                chatHistoryAdapter.getMessageDbId(lastPosition));
                    } catch (MessageNotFoundException ignored) {
                        Log.d(Settings.LOG_TAG, "Error while marking messages as read");
                    }
                    break;
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    }
}
