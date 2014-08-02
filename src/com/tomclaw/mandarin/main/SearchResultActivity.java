package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.im.SearchOptionsBuilder;
import com.tomclaw.mandarin.im.ShortBuddyInfo;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.im.icq.BuddySearchRequest;
import com.tomclaw.mandarin.im.icq.SearchAvatarRequest;
import com.tomclaw.mandarin.main.adapters.EndlessListAdapter;
import com.tomclaw.mandarin.main.adapters.SearchResultAdapter;

import java.util.List;
import java.util.Set;

/**
 * Created by Solkin on 06.07.2014.
 */
public class SearchResultActivity extends ChiefActivity {

    public static final String SEARCH_OPTIONS = "search_options";

    private int accountDbId;
    private SearchOptionsBuilder builder;
    private SearchResultAdapter searchAdapter;
    private ViewSwitcher emptyViewSwitcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.search_result_activity);

        if (!parseIntent(getIntent())) {
            finish();
            return;
        }

        emptyViewSwitcher = (ViewSwitcher) findViewById(android.R.id.empty);

        searchAdapter = new SearchResultAdapter(this, new EndlessListAdapter.EndlessAdapterListener() {
            @Override
            public void onLoadMoreItems(int offset) {
                requestItems(offset);
            }
        });
        ListView searchResultList = (ListView) findViewById(R.id.search_result_list);
        searchResultList.setEmptyView(emptyViewSwitcher);
        searchResultList.setAdapter(searchAdapter);
        requestItems(0);
        searchResultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ShortBuddyInfo info = searchAdapter.getItem(position);
                if (info != null) {
                    showBuddyInfo(info);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
        }
        return true;
    }

    private void requestItems(final int offset) {
        TaskExecutor.getInstance().execute(new ServiceTask(this) {
            @Override
            public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
                String appSession = interaction.getAppSession();
                RequestHelper.requestSearch(getContentResolver(), appSession, accountDbId, builder, offset);
            }

            @Override
            public void onFailBackground() {
                onSearchRequestError();
            }
        });
    }

    private boolean parseIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        // Checking for bundle condition.
        if (bundle != null) {
            accountDbId = bundle.getInt(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
            builder = (SearchOptionsBuilder) bundle.getSerializable(SEARCH_OPTIONS);
            return true;
        }
        return false;
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // Buddy search request data.
        boolean isResultPresent = !intent.getBooleanExtra(BuddySearchRequest.NO_SEARCH_RESULT_CASE, false);
        int requestAccountDbId = intent.getIntExtra(BuddySearchRequest.ACCOUNT_DB_ID, GlobalProvider.ROW_INVALID);
        SearchOptionsBuilder requestSearchOptions =
                (SearchOptionsBuilder) intent.getSerializableExtra(BuddySearchRequest.SEARCH_OPTIONS);
        // Search avatar request data.
        String avatarBuddyId = intent.getStringExtra(SearchAvatarRequest.BUDDY_ID);
        String avatarHash = intent.getStringExtra(SearchAvatarRequest.BUDDY_AVATAR_HASH);
        // Checking for request is the same.
        if (requestAccountDbId == accountDbId && requestSearchOptions != null &&
                ((Object) requestSearchOptions).equals(builder)) {
            int total = intent.getIntExtra(BuddySearchRequest.SEARCH_RESULT_TOTAL, 0);
            int offset = intent.getIntExtra(BuddySearchRequest.SEARCH_RESULT_OFFSET, 0);
            // Checking for result present and total count is positive.
            if (isResultPresent && total > 0) {
                Bundle bundle = intent.getBundleExtra(BuddySearchRequest.SEARCH_RESULT_BUNDLE);
                Set<String> buddyIds = bundle.keySet();
                for (String buddyId : buddyIds) {
                    ShortBuddyInfo info = (ShortBuddyInfo) bundle.getSerializable(buddyId);
                    Log.d(Settings.LOG_TAG, info.getBuddyId() + " [" + info.getBuddyNick() + "]");
                    searchAdapter.appendResult(info);
                }
                searchAdapter.setMoreItemsAvailable(total > offset + buddyIds.size());
                searchAdapter.notifyDataSetChanged();
            } else {
                Log.d(Settings.LOG_TAG, "No result case :(");
                if (searchAdapter.isEmpty()) {
                    onSearchRequestNoResult();
                }
                searchAdapter.setMoreItemsAvailable(false);
                searchAdapter.notifyDataSetChanged();
            }
        } else if (requestAccountDbId == accountDbId && !TextUtils.isEmpty(avatarBuddyId) &&
                !TextUtils.isEmpty(avatarHash)) {
            List<ShortBuddyInfo> shortInfoList = searchAdapter.getItems();
            for (ShortBuddyInfo info : shortInfoList) {
                if (TextUtils.equals(info.getBuddyId(), avatarBuddyId)) {
                    info.setAvatarHash(avatarHash);
                    searchAdapter.notifyDataSetChanged();
                    break;
                }
            }
        } else {
            Log.d(Settings.LOG_TAG, "Another search request with another account db id.");
        }
    }

    private void onSearchRequestNoResult() {
        emptyViewSwitcher.showNext();
    }

    private void onSearchRequestError() {
        Toast.makeText(this, R.string.search_buddy_error, Toast.LENGTH_SHORT).show();
    }

    private void showBuddyInfo(ShortBuddyInfo info) {
        startActivity(new Intent(this, BuddyInfoActivity.class)
                        .putExtra(BuddyInfoRequest.ACCOUNT_DB_ID, accountDbId)
                        .putExtra(BuddyInfoRequest.BUDDY_ID, info.getBuddyId())
                        .putExtra(BuddyInfoRequest.BUDDY_NICK, info.getBuddyNick())
                        .putExtra(BuddyInfoRequest.BUDDY_AVATAR_HASH, info.getAvatarHash())
        );
    }
}
