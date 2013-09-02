package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import com.tomclaw.mandarin.main.adapters.ChatDialogsAdapter;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

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
    private CharSequence drawerTitle;
    private CharSequence title;

    private ChatDialogsAdapter chatDialogsAdapter;
    private ListView chatList;
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
        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        chatList = (ListView) findViewById(R.id.chat_list);
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this,
                getLoaderManager(), buddyDbId);
        chatList.setAdapter(chatHistoryAdapter);

        chatList.setMultiChoiceModeListener(new MultiChoiceModeListener());

        chatDialogsAdapter = new ChatDialogsAdapter(this, getLoaderManager());

        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(chatDialogsAdapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        title = drawerTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer_dark,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
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
                String message = messageText.getText().toString();
                if (TextUtils.isEmpty(message)) {
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

    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

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

    private class SelectionHelper {

        private Map<Integer, String> selectionMap;

        public SelectionHelper() {
            selectionMap = new TreeMap<Integer, String>();
        }

        public void finish() {
            // Clearing all.
            selectionMap.clear();
        }

        public String buildSelection() {
            // Building selected messages.
            StringBuilder selectionBuilder = new StringBuilder();
            Collection<String> selection = selectionMap.values();
            for (String message : selection) {
                selectionBuilder.append(message).append('\n').append('\n');
            }
            return selectionBuilder.toString().trim();
        }

        public void setSelection(int position, String value) {
            if (TextUtils.isEmpty(value)) {
                selectionMap.remove(position);
            } else {
                selectionMap.put(position, value);
            }
        }
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.setSelection(position, checked ? chatHistoryAdapter.getItemText(position) : null);
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
            String selection = selectionHelper.buildSelection();
            switch (item.getItemId()) {
                case R.id.message_copy:
                    ClipboardManager clipboardManager = (ClipboardManager)
                            getSystemService(CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", selection));
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

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.finish();
            chatList.clearChoices();
        }
    }
}
