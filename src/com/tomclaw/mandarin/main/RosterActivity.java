package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.main.adapters.RosterAlphabetAdapter;
import com.tomclaw.mandarin.main.tasks.AccountProviderTask;
import com.tomclaw.mandarin.main.tasks.BuddyRemoveTask;
import com.tomclaw.mandarin.util.SelectionHelper;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

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

        // Alphabetical list.
        StickyListHeadersListView generalList = (StickyListHeadersListView) findViewById(R.id.roster_list_view);
        final RosterAlphabetAdapter generalAdapter =
                new RosterAlphabetAdapter(this, getLoaderManager(), getFilterValue());
        generalList.setAdapter(generalAdapter);
        generalList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int buddyDbId = generalAdapter.getBuddyDbId(position);
                Log.d(Settings.LOG_TAG, "Opening dialog with buddy (db id): " + buddyDbId);
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

        final ActionBar mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(false);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(mActionBar.getThemedContext(),
                R.array.roster_filter_strings, android.R.layout.simple_spinner_dropdown_item);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(filterAdapter, new ActionBar.OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                setFilterValue(itemPosition);
                generalAdapter.setRosterFilter(itemPosition);
                generalAdapter.initLoader();
                return true;
            }
        });
        mActionBar.setSelectedNavigationItem(generalAdapter.getRosterFilter());

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
        // Configure the search info and add any event listeners
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.menu_add_buddy: {
                SearchAccountCallback callback = new SearchAccountCallback(this);
                AccountProviderTask task = new AccountProviderTask(this, callback);
                TaskExecutor.getInstance().execute(task);
                return true;
            }
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
                .putInt(ROSTER_FILTER_PREFERENCE, filterValue).commit();
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Integer> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, (int) id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
            updateMenu(mode, mode.getMenu());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper<Integer, Integer>();
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

                View view = getLayoutInflater().inflate(R.layout.buddy_rename_dialog, null);

                final EditText buddyNameText = (EditText) view.findViewById(R.id.buddy_name_edit);
                buddyNameText.setText(buddyPreviousNick);
                buddyNameText.setSelection(buddyNameText.length());

                AlertDialog alertDialog = new AlertDialog.Builder(RosterActivity.this)
                        .setTitle(R.string.edit_buddy_name_title)
                        .setView(view)
                        .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String buddySatisfiedNick = buddyNameText.getText().toString();

                                QueryHelper.modifyBuddyNick(getContentResolver(), buddyDbId, buddySatisfiedNick, true);

                                RequestHelper.requestRename(getContentResolver(), accountDbId, buddyId,
                                        buddyPreviousNick, buddySatisfiedNick);
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
            final Collection<Integer> selectedBuddies = new ArrayList<Integer>(buddyDbIds);
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
            this.weakContext = new WeakReference<Context>(context);
        }

        @Override
        public void onAccountSelected(int accountDbId) {
            Log.d(Settings.LOG_TAG, "Account selected: " + accountDbId);
            Context context = weakContext.get();
            if(context != null) {
                Intent intent = new Intent(context, SearchActivity.class);
                intent.putExtra(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                context.startActivity(intent);
            }
        }

        @Override
        public void onNoActiveAccounts() {
            Log.d(Settings.LOG_TAG, "No active accounts.");
            Context context = weakContext.get();
            if(context != null) {
                Toast.makeText(context, R.string.no_active_accounts, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
