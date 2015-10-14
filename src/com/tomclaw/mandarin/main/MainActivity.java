package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.icq.IntroActivity;
import com.tomclaw.mandarin.main.tasks.AccountProviderTask;
import com.tomclaw.mandarin.main.views.AccountsDrawerLayout;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.SelectionHelper;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;

public class MainActivity extends ChiefActivity {

    private RosterDialogsAdapter dialogsAdapter;
    private ListView dialogsList;
    private Toolbar toolbar;
    private FloatingActionButton actionButton;

    private AccountsDrawerLayout drawerLayout;
    private MultiChoiceModeListener multiChoiceModeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        // Check for start helper needs to be shown.
        if (PreferenceHelper.isShowStartHelper(this)) {
            // This will start account creation.
            Intent accountAddIntent = new Intent(this, IntroActivity.class);
            accountAddIntent.putExtra(IntroActivity.EXTRA_START_HELPER, true);
            overridePendingTransition(0, 0);
            startActivity(accountAddIntent);
            finish();
            return;
        }

        setContentView(R.layout.main_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.dialogs);
        setSupportActionBar(toolbar);

        drawerLayout = (AccountsDrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.init(this, toolbar);
        drawerLayout.setTitle(getString(R.string.dialogs));
        drawerLayout.setDrawerTitle(getString(R.string.accounts));

        actionButton = (FloatingActionButton) findViewById(R.id.fab);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RosterActivity.class);
                startActivity(intent);
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0); // get rid of margins since shadow area is now the margin
            actionButton.setLayoutParams(p);
        }

        // Dialogs list.
        dialogsAdapter = new RosterDialogsAdapter(this, getLoaderManager());
        dialogsAdapter.setAdapterCallback(new RosterDialogsAdapter.RosterAdapterCallback() {
            @Override
            public void onRosterLoadingStarted() {
                // Disable placeholder when loading started.
                dialogsList.setEmptyView(null);
            }

            @Override
            public void onRosterEmpty() {
                // Show empty view only for really empty list.
                dialogsList.setEmptyView(findViewById(android.R.id.empty));
            }

            @Override
            public void onRosterUpdate() {
                if (multiChoiceModeListener != null) {
                    multiChoiceModeListener.updateMenu();
                }
            }
        });
        dialogsList = (ListView) findViewById(R.id.chats_list_view);
        dialogsList.setAdapter(dialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int buddyDbId = dialogsAdapter.getBuddyDbId(position);
                Logger.log("Check out dialog with buddy (db id): " + buddyDbId);
                Intent intent = new Intent(MainActivity.this, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);
            }
        });
        dialogsList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        Logger.log("main activity start time: " + (System.currentTimeMillis() - time));

        checkNfcIntent();
    }

    private void checkNfcIntent() {
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = (NdefMessage) rawMessages[0];
            for (NdefRecord record : message.getRecords()) {
                // Check for only Mandarin's mime-type record and media type.
                if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                        Arrays.equals(record.getType(), Settings.MIME_TYPE.getBytes())) {
                    String json = new String(record.getPayload());
                    NfcBuddyInfo nfcBuddyInfo = GsonSingleton.getInstance().fromJson(json, NfcBuddyInfo.class);
                    BuddyInfoAccountCallback callback = new BuddyInfoAccountCallback(this, nfcBuddyInfo);
                    AccountProviderTask task = new AccountProviderTask(this, callback);
                    TaskExecutor.getInstance().execute(task);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForCrashes();
    }

    private void checkForCrashes() {
        CrashManager.register(this, "16283e6bea480850d8f4c1b41d7a74be", new CrashManagerListener() {
            public boolean shouldAutoUploadCrashes() {
                return true;
            }
        });
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
        if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
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
            case R.id.settings: {
                openSettings();
                return true;
            }
            case R.id.info: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
            // Close drawer if opened.
            drawerLayout.closeDrawers();
        } else {
            // Finish otherwise.
            finish();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        drawerLayout.setTitle(title.toString());
        toolbar.setTitle(title);
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
        Logger.log("onCoreServiceIntent");
    }

    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer> selectionHelper;
        private ActionMode actionMode;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged((int) id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
            updateMenu(mode, mode.getMenu());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper<>();
            multiChoiceModeListener = this;
            actionMode = mode;
            updateMenu(mode, menu);
            return true;
        }

        public void updateMenu() {
            updateMenu(actionMode, actionMode.getMenu());
        }

        private void updateMenu(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            menu.clear();
            // Checking for unread dialogs.
            int menuRes = R.menu.chat_list_edit_menu;
            Collection<Integer> selectedIds = selectionHelper.getSelectedIds();
            dialogsAdapter.getBuddyCursor().moveToFirst();
            BuddyCursor cursor = dialogsAdapter.getBuddyCursor();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                for (int selectedId : selectedIds) {
                    if (selectedId == cursor.getBuddyDbId() &&
                            cursor.getBuddyUnreadCount() > 0) {
                        menuRes = R.menu.chat_list_unread_edit_menu;
                        break;
                    }
                }
            }
            inflater.inflate(menuRes, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.select_all_chats_menu: {
                    for (int c = 0; c < dialogsAdapter.getCount(); c++) {
                        dialogsList.setItemChecked(c, true);
                    }
                    return false;
                }
                case R.id.mark_as_read_chat_menu: {
                    try {
                        QueryHelper.readAllMessages(getContentResolver(), selectionHelper.getSelectedIds());
                    } catch (Exception ignored) {
                        // Nothing to do in this case.
                    }
                    break;
                }
                case R.id.close_chat_menu: {
                    try {
                        QueryHelper.modifyDialogs(getContentResolver(), selectionHelper.getSelectedIds(), false);
                    } catch (Exception ignored) {
                        // Nothing to do in this case.
                    }
                    break;
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
            multiChoiceModeListener = null;
        }
    }

    private class BuddyInfoAccountCallback implements AccountProviderTask.AccountProviderCallback {

        private final NfcBuddyInfo nfcBuddyInfo;
        private final WeakReference<Context> weakContext;

        private BuddyInfoAccountCallback(Context context, NfcBuddyInfo nfcBuddyInfo) {
            this.nfcBuddyInfo = nfcBuddyInfo;
            this.weakContext = new WeakReference<>(context);
        }

        @Override
        public void onAccountSelected(int accountDbId) {
            Logger.log("Account selected: " + accountDbId);
            Context context = weakContext.get();
            if (context != null) {
                context.startActivity(new Intent(context, BuddyInfoActivity.class)
                                .putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, accountDbId)
                                .putExtra(BuddyInfoRequest.BUDDY_ID, nfcBuddyInfo.getBuddyId())
                                .putExtra(BuddyInfoRequest.BUDDY_NICK, nfcBuddyInfo.getBuddyNick())
                );
            }
        }

        @Override
        public void onNoActiveAccounts() {
            Logger.log("No active accounts.");
            Context context = weakContext.get();
            if (context != null) {
                Toast.makeText(context, R.string.no_active_accounts, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
