package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StrictBuddy;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.main.icq.IntroActivity;
import com.tomclaw.mandarin.main.views.AccountsDrawerLayout;
import com.tomclaw.mandarin.util.AppCenterHelper;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.preferences.PreferenceHelper;

import java.util.Collection;
import java.util.Collections;

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
        actionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RosterActivity.class);
            startActivity(intent);
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
            public void onItemStateChanged(StrictBuddy buddy) {
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
            public void onLongClicked(StrictBuddy buddy, SelectionHelper<StrictBuddy> selectionHelper) {
                if (selectionHelper.setSelectionMode(true)) {
                    actionCallback = new MultiChoiceActionCallback(selectionHelper);
                    actionMode = toolbar.startActionMode(actionCallback);
                    selectionHelper.setChecked(buddy);
                    onItemStateChanged(buddy);
                }
            }
        });
        dialogsAdapter.setClickListener(buddy -> {
            Logger.log("Check out dialog with: " + buddy.toString());
            Intent intent = new Intent(MainActivity.this, ChatActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Buddy.KEY_STRUCT, buddy);
            startActivity(intent);
        });
        AppCenterHelper.checkForCrashes(getApplication());
        Logger.log("main activity start time: " + (System.currentTimeMillis() - time));
    }

    @Override
    public void onResume() {
        super.onResume();
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

        private SelectionHelper<StrictBuddy> selectionHelper;

        MultiChoiceActionCallback(SelectionHelper<StrictBuddy> selectionHelper) {
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
            Collection<StrictBuddy> buddies = selectionHelper.getSelected();
            dialogsAdapter.getBuddyCursor().moveToFirst();
            BuddyCursor cursor = dialogsAdapter.getBuddyCursor();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                for (Buddy buddy : buddies) {
                    if (buddy.equals(cursor.toBuddy()) &&
                            cursor.getUnreadCount() > 0) {
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
                        StrictBuddy buddy = dialogsAdapter.getBuddy(c);
                        selectionHelper.setChecked(buddy);
                    }
                    onItemCheckedStateChanged(mode);
                    dialogsAdapter.notifyDataSetChanged();
                    return false;
                }
                case R.id.close_chat_menu: {
                    try {
                        ContentResolver contentResolver = getContentResolver();
                        DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                        Collection<Buddy> selectedBuddies = Collections.unmodifiableCollection(selectionHelper.getSelected());
                        QueryHelper.modifyDialogs(databaseLayer, selectedBuddies, false);
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

}
