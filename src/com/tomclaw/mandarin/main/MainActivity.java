package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.views.AccountsDrawerLayout;
import com.tomclaw.mandarin.util.SelectionHelper;

public class MainActivity extends ChiefActivity {

    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String MARKET_DEVELOPER_URI = "market://search?q=";
    private static final String GOOGLE_PLAY_DETAILS_URI = "http://play.google.com/store/apps/details?id=";
    private static final String GOOGLE_PLAY_DEVELOPER_URI = "http://play.google.com/store/apps/developer?id=";

    public static final int ADDING_ACTIVITY_REQUEST_CODE = 1;

    private RosterDialogsAdapter dialogsAdapter;
    private ListView dialogsList;

    private AccountsDrawerLayout drawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for start helper needs to be shown.
        if (PreferenceHelper.isShowStartHelper(this)) {
            // This will start
            Intent accountAddIntent = new Intent(this, AccountAddActivity.class);
            accountAddIntent.putExtra(AccountAddActivity.EXTRA_CLASS_NAME, IcqAccountRoot.class.getName());
            accountAddIntent.putExtra(AccountAddActivity.EXTRA_START_HELPER, true);
            overridePendingTransition(0, 0);
            startActivity(accountAddIntent);
            finish();
            return;
        }

        setContentView(R.layout.main_activity);

        final ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setTitle(R.string.dialogs);

        drawerLayout = (AccountsDrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.init(this);
        drawerLayout.setTitle(getString(R.string.dialogs));
        drawerLayout.setDrawerTitle(getString(R.string.accounts));

        // Dialogs list.
        dialogsAdapter = new RosterDialogsAdapter(this, getLoaderManager());
        dialogsList = (ListView) findViewById(R.id.chats_list_view);
        dialogsList.setAdapter(dialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int buddyDbId = dialogsAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Check out dialog with buddy (db id): " + buddyDbId);
                Intent intent = new Intent(MainActivity.this, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);
            }
        });
        dialogsList.setMultiChoiceModeListener(new MultiChoiceModeListener());

        dialogsList.setEmptyView(findViewById(android.R.id.empty));
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
        int menuResource;
        // Checking for drawer is initialized, opened and show such menu.
        if(drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
            menuResource = R.menu.accounts_list_menu;
        } else {
            menuResource = R.menu.main_activity_menu;
        }
        getMenuInflater().inflate(menuResource, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerLayout.onToggleOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.accounts: {
                drawerLayout.openDrawer(Gravity.START);
                return true;
            }
            case R.id.create_dialog: {
                Intent intent = new Intent(this, RosterActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.settings: {
                openSettings();
                return true;
            }
            case R.id.rate_application: {
                rateApplication();
                return true;
            }
            case R.id.all_projects: {
                allProjects();
                return true;
            }
            case R.id.info: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.add_account_menu:
                Intent accountAddIntent = new Intent(this, AccountAddActivity.class);
                accountAddIntent.putExtra(AccountAddActivity.EXTRA_CLASS_NAME, IcqAccountRoot.class.getName());
                startActivityForResult(accountAddIntent, ADDING_ACTIVITY_REQUEST_CODE);
                return true;
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onCoreServiceReady() {
    }

    @Override
    public void onCoreServiceDown() {
        Log.d(Settings.LOG_TAG, "onCoreServiceDown");
    }

    @Override
    public void setTitle(CharSequence title) {
        drawerLayout.setTitle(title.toString());
        getActionBar().setTitle(title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerLayout.syncToggleState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerLayout.onToggleConfigurationChanged(newConfig);
    }

    public void onCoreServiceIntent(Intent intent) {
        Log.d(Settings.LOG_TAG, "onCoreServiceIntent");
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void rateApplication() {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_DETAILS_URI + appPackageName)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_DETAILS_URI + appPackageName)));
        }
    }

    private void allProjects() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_DEVELOPER_URI + Settings.DEVELOPER_NAME)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_DEVELOPER_URI + Settings.DEVELOPER_NAME)));
        }
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Integer> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, (int) id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper<Integer, Integer>();
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.chat_list_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.close_chat_menu: {
                    try {
                        QueryHelper.modifyDialogs(getContentResolver(), selectionHelper.getSelectedIds(), false);
                    } catch (Exception ignored) {
                        // Nothing to do in this case.
                    }
                    break;
                }
                case R.id.select_all_chats_menu: {
                    for (int c = 0; c < dialogsAdapter.getCount(); c++) {
                        dialogsList.setItemChecked(c, true);
                    }
                    return false;
                }
                default: {
                    return false;
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }
    }
}
