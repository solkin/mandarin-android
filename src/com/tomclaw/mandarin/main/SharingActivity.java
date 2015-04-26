package com.tomclaw.mandarin.main;

import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.main.adapters.RosterSharingAdapter;
import com.tomclaw.mandarin.main.adapters.RosterStickyAdapter;
import com.tomclaw.mandarin.util.Logger;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Igor on 23.04.2015.
 */
public class SharingActivity extends ChiefActivity {

    public static final String EXTRA_SHARING_DATA = "sharing_data";

    private SharingData sharingData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse intent we runned with.
        parseIntent();

        setContentView(R.layout.sharing_activity);

        // Sticky list.
        StickyListHeadersListView generalList = (StickyListHeadersListView) findViewById(R.id.sharing_list_view);
        final RosterStickyAdapter generalAdapter = new RosterSharingAdapter(this,
                getLoaderManager());
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
                    Intent intent = new Intent(SharingActivity.this, ChatActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                            .putExtra(EXTRA_SHARING_DATA, sharingData);
                    startActivity(intent);
                    finish();
                } catch (Exception ignored) {
                    // Nothing to do in this case.
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public void setIntent(Intent newIntent) {
        super.setIntent(newIntent);
        parseIntent();
    }

    private void parseIntent() {
        sharingData = new SharingData(getIntent());
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }
}
