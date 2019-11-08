package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.main.adapters.RosterSharingAdapter;
import com.tomclaw.mandarin.main.adapters.RosterStickyAdapter;
import com.tomclaw.mandarin.main.icq.IntroActivity;
import com.tomclaw.mandarin.util.Logger;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Igor on 23.04.2015.
 */
public class SharingActivity extends ChiefActivity {

    public static final String EXTRA_SHARING_DATA = "sharing_data";

    private SharingData sharingData;
    private RosterStickyAdapter generalAdapter;
    private SearchView.OnQueryTextListener onQueryTextListener;
    private DatabaseLayer databaseLayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        databaseLayer = ContentResolverLayer.from(getContentResolver());
        super.onCreate(savedInstanceState);

        // Parse intent we runned with.
        parseIntent();

        setContentView(R.layout.sharing_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Sticky list.
        StickyListHeadersListView generalList = findViewById(R.id.sharing_list_view);
        generalAdapter = new RosterSharingAdapter(this,
                getLoaderManager());
        // Accepting adapter.
        generalList.setAdapter(generalAdapter);
        generalList.setOnItemClickListener((parent, view, position, id) -> {
            Buddy buddy = generalAdapter.getBuddy(position);
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            Logger.log("Opening dialog with buddy: " + buddyId + "(from account db id: " + accountDbId + ")");
            try {
                // Trying to open dialog with this buddy.
                QueryHelper.modifyDialog(databaseLayer, buddy, true);
                // Open chat dialog for this buddy.
                Intent intent = new Intent(SharingActivity.this, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Buddy.KEY_STRUCT, buddy)
                        .putExtra(EXTRA_SHARING_DATA, sharingData);
                startActivity(intent);
                finish();
            } catch (Exception ignored) {
                // Nothing to do in this case.
            }
        });

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

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.country_code_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setQueryHint(menu.findItem(R.id.menu_search).getTitle());
        // Configure the search info and add event listener.
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
        }
        return false;
    }

    @Override
    public void setIntent(Intent newIntent) {
        super.setIntent(newIntent);
        parseIntent();
    }

    private void parseIntent() {
        sharingData = new SharingData(getIntent());
        if (!sharingData.isValid()) {
            Toast.makeText(this, R.string.invalid_file, Toast.LENGTH_SHORT).show();
            finish();
        } else if (QueryHelper.getAccountsCount(databaseLayer) == 0) {
            // This will start account creation.
            Intent accountAddIntent = new Intent(this, IntroActivity.class);
            accountAddIntent.putExtra(IntroActivity.EXTRA_START_HELPER, true);
            accountAddIntent.putExtra(IntroActivity.EXTRA_RELAY_INTENT, getIntent());
            overridePendingTransition(0, 0);
            startActivity(accountAddIntent);
            finish();
        }
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
