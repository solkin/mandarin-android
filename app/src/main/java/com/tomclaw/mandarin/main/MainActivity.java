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
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.icq.IntroActivity;
import com.tomclaw.mandarin.main.tasks.AccountProviderTask;
import com.tomclaw.mandarin.main.views.AccountsDrawerLayout;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;

import static com.tomclaw.mandarin.im.AccountRoot.AUTH_LOST;

public class MainActivity extends ChiefActivity {

    private RosterDialogsAdapter dialogsAdapter;
    private Toolbar toolbar;
    private ViewFlipper viewFlipper;

    private AccountsDrawerLayout drawerLayout;
    private MultiChoiceActionCallback actionCallback;
    private ActionMode actionMode;

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

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.dialogs);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.init(this, toolbar);
        drawerLayout.setTitle(getString(R.string.dialogs));
        drawerLayout.setDrawerTitle(getString(R.string.accounts));

        FloatingActionButton actionButton = findViewById(R.id.fab);
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

        viewFlipper = findViewById(R.id.roster_view_flipper);

        // Dialogs list.
        dialogsAdapter = new RosterDialogsAdapter(this, getLoaderManager());
        dialogsAdapter.setAdapterCallback(new RosterDialogsAdapter.RosterAdapterCallback() {
            @Override
            public void onRosterLoadingStarted() {
                // Disable placeholder when loading started.
                showRoster();
            }

            @Override
            public void onRosterEmpty() {
                // Show empty view only for really empty list.
                showEmpty();
            }

            @Override
            public void onRosterUpdate() {
                showRoster();
                if (actionCallback != null) {
                    actionCallback.updateMenu(actionMode, actionMode.getMenu());
                }
            }

            private void showRoster() {
                if (viewFlipper.getDisplayedChild() != 0) {
                    viewFlipper.setDisplayedChild(0);
                }
            }

            private void showEmpty() {
                if (viewFlipper.getDisplayedChild() != 1) {
                    viewFlipper.setDisplayedChild(1);
                }
            }
        });
        RecyclerView dialogsList = findViewById(R.id.chats_list_view);
        dialogsList.setHasFixedSize(true);
        dialogsList.setLayoutManager(new LinearLayoutManager(this));
        dialogsList.setItemAnimator(new DefaultItemAnimator());
        dialogsList.setAdapter(dialogsAdapter);
        dialogsAdapter.setSelectionModeListener(new RosterDialogsAdapter.SelectionModeListener() {

            @Override
            public void onItemStateChanged(int buddyDbId) {
                // Strange case, but let's check it to be sure.
                if (actionCallback != null && actionMode != null) {
                    actionCallback.onItemCheckedStateChanged(actionMode);
                    dialogsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected() {
                // Strange case, but let's check it to be sure.
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            @Override
            public void onLongClicked(int buddyDbId, SelectionHelper<Integer> selectionHelper) {
                if (selectionHelper.setSelectionMode(true)) {
                    actionCallback = new MultiChoiceActionCallback(selectionHelper);
                    actionMode = toolbar.startActionMode(actionCallback);
                    selectionHelper.setChecked(buddyDbId);
                    onItemStateChanged(buddyDbId);
                }
            }
        });
        dialogsAdapter.setClickListener(new RosterDialogsAdapter.ClickListener() {
            @Override
            public void onItemClicked(int buddyDbId) {
                Logger.log("Check out dialog with buddy (db id): " + buddyDbId);
                Intent intent = new Intent(MainActivity.this, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);
            }
        });
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
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
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
                drawerLayout.openDrawer(GravityCompat.START);
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
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerLayout.onToggleConfigurationChanged(newConfig);
    }

    public void onCoreServiceIntent(Intent intent) {
        Logger.log("onCoreServiceIntent");
        if (intent.getBooleanExtra(AUTH_LOST, false)) {
            startActivity(new Intent(this, IntroActivity.class));
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private class MultiChoiceActionCallback implements ActionMode.Callback {

        private final SelectionHelper<Integer> selectionHelper;

        MultiChoiceActionCallback(SelectionHelper<Integer> selectionHelper) {
            this.selectionHelper = selectionHelper;
        }

        void onItemCheckedStateChanged(ActionMode mode) {
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
            updateMenu(mode, mode.getMenu());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            updateMenu(mode, menu);
            return true;
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
                    for (int c = 0; c < dialogsAdapter.getItemCount(); c++) {
                        int buddyDbId = dialogsAdapter.getBuddyDbId(c);
                        selectionHelper.setChecked(buddyDbId);
                    }
                    onItemCheckedStateChanged(mode);
                    dialogsAdapter.notifyDataSetChanged();
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
            selectionHelper.setSelectionMode(false);
            dialogsAdapter.notifyDataSetChanged();
        }
    }

    private static class BuddyInfoAccountCallback implements AccountProviderTask.AccountProviderCallback {

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
