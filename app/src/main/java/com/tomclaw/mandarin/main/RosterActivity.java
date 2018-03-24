package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.main.adapters.RosterAlphabetAdapter;
import com.tomclaw.mandarin.main.adapters.RosterGroupAdapter;
import com.tomclaw.mandarin.main.adapters.RosterStatusAdapter;
import com.tomclaw.mandarin.main.adapters.RosterStickyAdapter;
import com.tomclaw.mandarin.main.tasks.AccountProviderTask;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.tasks.BuddyRemoveTask;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/8/13
 * Time: 8:11 PM
 */
public class RosterActivity extends ChiefActivity {

    private static final String ROSTER_FILTER_PREFERENCE = "roster_filter";
    private SearchView.OnQueryTextListener onQueryTextListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.roster_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton actionButton = findViewById(R.id.fab);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchAccountCallback callback = new SearchAccountCallback(RosterActivity.this);
                AccountProviderTask task = new AccountProviderTask(RosterActivity.this, callback);
                TaskExecutor.getInstance().execute(task);
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0); // get rid of margins since shadow area is now the margin
            actionButton.setLayoutParams(p);
        }

        // Sticky list.
        StickyListHeadersListView generalList = findViewById(R.id.roster_list_view);
        final RosterStickyAdapter generalAdapter;
        // Checking for adapter mode.
        String rosterMode = PreferenceHelper.getRosterMode(this);
        if (TextUtils.equals(rosterMode, getString(R.string.roster_mode_groups))) {
            generalAdapter = new RosterGroupAdapter(this, getLoaderManager(), getFilterValue());
        } else if (TextUtils.equals(rosterMode, getString(R.string.roster_mode_status))) {
            generalAdapter = new RosterStatusAdapter(this, getLoaderManager(), getFilterValue());
        } else {
            generalAdapter = new RosterAlphabetAdapter(this, getLoaderManager(), getFilterValue());
        }
        // Accepting adapter.
        generalList.setAdapter(generalAdapter);
        generalList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int buddyDbId = generalAdapter.getBuddyDbId(position);
                Logger.log("Opening dialog with buddy (db id): " + buddyDbId);
                try {
                    // Trying to open dialog with this buddy.
                    QueryHelper.modifyDialog(getContentResolver(), buddyDbId, true);
                    // Open chat dialog for this buddy.
                    Intent intent = new Intent(RosterActivity.this, ChatActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
            }
        });
        generalList.getWrappedList().setMultiChoiceModeListener(new MultiChoiceModeListener());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(),
                R.array.roster_filter_strings, android.R.layout.simple_spinner_dropdown_item);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(filterAdapter, new ActionBar.OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                setFilterValue(itemPosition);
                generalAdapter.setRosterFilter(itemPosition);
                generalAdapter.initLoader();
                return true;
            }
        });
        actionBar.setSelectedNavigationItem(generalAdapter.getRosterFilter());

        onQueryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                generalAdapter.getFilter().filter(newText);
                return false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.roster_activity_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setQueryHint(menu.findItem(R.id.menu_search).getTitle());
        // Configure the search info and add any event listeners
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    private int getFilterValue() {
        return PreferenceManager.getDefaultSharedPreferences(RosterActivity.this)
                .getInt(ROSTER_FILTER_PREFERENCE, RosterAlphabetAdapter.FILTER_ALL_BUDDIES);
    }

    private void setFilterValue(int filterValue) {
        PreferenceManager.getDefaultSharedPreferences(RosterActivity.this).edit()
                .putInt(ROSTER_FILTER_PREFERENCE, filterValue).apply();
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer> selectionHelper;

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
            updateMenu(mode, menu);
            return true;
        }

        private void updateMenu(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            menu.clear();
            int menuRes = (selectionHelper.getSelectedCount() > 1) ?
                    R.menu.roster_edit_multiple_menu : R.menu.roster_edit_single_menu;
            inflater.inflate(menuRes, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.buddy_info_menu: {
                    int buddyDbId = selectionHelper.getSelectedIds().iterator().next();
                    TaskExecutor.getInstance().execute(new BuddyInfoTask(RosterActivity.this, buddyDbId));
                    break;
                }
                case R.id.rename_buddy_menu: {
                    int buddyDbId = selectionHelper.getSelectedIds().iterator().next();
                    renameSelectedBuddy(buddyDbId);
                    break;
                }
                case R.id.remove_buddy_menu: {
                    removeSelectedBuddies(selectionHelper.getSelectedIds());
                    break;
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }

        private void renameSelectedBuddy(final int buddyDbId) {
            BuddyCursor buddyCursor = null;
            try {
                buddyCursor = QueryHelper.getBuddyCursor(getContentResolver(), buddyDbId);
                final int accountDbId = buddyCursor.getBuddyAccountDbId();
                final String buddyId = buddyCursor.getBuddyId();
                final String buddyPreviousNick = buddyCursor.getBuddyNick();
                final boolean isPersistent = (buddyCursor.getBuddyGroupId() != GlobalProvider.GROUP_ID_RECYCLE);

                View view = getLayoutInflater().inflate(R.layout.buddy_rename_dialog, null);

                final EditText buddyNameText = view.findViewById(R.id.buddy_name_edit);
                buddyNameText.setText(buddyPreviousNick);
                buddyNameText.setSelection(buddyNameText.length());

                AlertDialog alertDialog = new AlertDialog.Builder(RosterActivity.this)
                        .setTitle(R.string.edit_buddy_name_title)
                        .setView(view)
                        .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String buddySatisfiedNick = buddyNameText.getText().toString();
                                // Renaming only if buddy nicks are different.
                                if (!TextUtils.equals(buddyPreviousNick, buddySatisfiedNick)) {
                                    QueryHelper.modifyBuddyNick(getContentResolver(), buddyDbId,
                                            buddySatisfiedNick, isPersistent);
                                    if (isPersistent) {
                                        RequestHelper.requestRename(getContentResolver(), accountDbId, buddyId,
                                                buddyPreviousNick, buddySatisfiedNick);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(R.string.not_now, null)
                        .create();
                alertDialog.show();
            } catch (BuddyNotFoundException e) {
                Toast.makeText(RosterActivity.this, R.string.no_buddy_in_roster, Toast.LENGTH_SHORT).show();
            } finally {
                if (buddyCursor != null) {
                    buddyCursor.close();
                }
            }
        }

        private void removeSelectedBuddies(Collection<Integer> buddyDbIds) {
            final Collection<Integer> selectedBuddies = new ArrayList<>(buddyDbIds);
            boolean isMultiple = buddyDbIds.size() > 1;
            String message;
            if (isMultiple) {
                message = getString(R.string.remove_buddies_message, buddyDbIds.size());
            } else {
                message = getString(R.string.remove_buddy_message);
            }
            AlertDialog alertDialog = new AlertDialog.Builder(RosterActivity.this)
                    .setTitle(isMultiple ? R.string.remove_buddies_title : R.string.remove_buddy_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BuddyRemoveTask task = new BuddyRemoveTask(RosterActivity.this, selectedBuddies);
                            TaskExecutor.getInstance().execute(task);
                        }
                    })
                    .setNegativeButton(R.string.do_not_remove, null)
                    .create();
            alertDialog.show();
        }
    }

    private class SearchAccountCallback implements AccountProviderTask.AccountProviderCallback {

        WeakReference<Context> weakContext;

        private SearchAccountCallback(Context context) {
            this.weakContext = new WeakReference<>(context);
        }

        @Override
        public void onAccountSelected(int accountDbId) {
            Logger.log("Account selected: " + accountDbId);
            Context context = weakContext.get();
            if (context != null) {
                Intent intent = new Intent(context, SearchActivity.class);
                intent.putExtra(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                context.startActivity(intent);
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
