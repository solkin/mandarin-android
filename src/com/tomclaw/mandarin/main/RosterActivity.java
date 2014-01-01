package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.RosterAlphabetAdapter;
import com.tomclaw.mandarin.main.fragments.ChatDialogsFragment;
import com.tomclaw.mandarin.main.fragments.ChatFragment;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/8/13
 * Time: 8:11 PM
 */
public class RosterActivity extends ChiefActivity {

    private static final String ROSTER_FILTER_PREFERENCE = "roster_filter";

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

                    if (isDualPane()) {
                        Intent intent = new Intent(RosterActivity.this, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                        startActivity(intent);
                    } else {
                        // Open chat dialog for this buddy.
                        Intent intent = new Intent(RosterActivity.this, ChatActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                        startActivity(intent);
                    }
                    finish();
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
            }
        });

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.roster_activity_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Configure the search info and add any event listeners
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
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
    public void onCoreServiceReady() {
    }

    @Override
    public void onCoreServiceDown() {
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

    private boolean isDualPane() {
        return (findViewById(R.id.signal_frame) != null);
    }
}
