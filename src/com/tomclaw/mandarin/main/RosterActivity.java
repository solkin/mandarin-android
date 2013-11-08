package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.adapters.RosterAlphabetAdapter;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/8/13
 * Time: 8:11 PM
 */
public class RosterActivity extends ChiefActivity {

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
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCoreServiceReady() {
        setContentView(R.layout.roster_activity);

        final ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Alphabetical list.
        StickyListHeadersListView generalList = (StickyListHeadersListView) findViewById(R.id.roster_list_view);
        final RosterAlphabetAdapter generalAdapter = new RosterAlphabetAdapter(this, getLoaderManager());
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
                    Intent intent = new Intent(RosterActivity.this, ChatActivity.class);
                    intent.putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    // Nothing to do in this case.
                }
            }
        });
    }

    @Override
    public void onCoreServiceDown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
