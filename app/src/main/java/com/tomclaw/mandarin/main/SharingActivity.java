package com.tomclaw.mandarin.main;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
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

import static com.tomclaw.mandarin.util.PermissionsHelper.hasPermissions;

/**
 * Created by Igor on 23.04.2015.
 */
public class SharingActivity extends ChiefActivity {

    public static final String EXTRA_SHARING_DATA = "sharing_data";

    private static final int REQUEST_SHARE_FILE = 1;

    private StickyListHeadersListView generalList;
    private SharingData sharingData;
    private RosterStickyAdapter generalAdapter;
    private SearchView.OnQueryTextListener onQueryTextListener;
    private DatabaseLayer databaseLayer;

    private Buddy selectedBuddy;

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
        generalList = findViewById(R.id.sharing_list_view);
        generalAdapter = new RosterSharingAdapter(this,
                getLoaderManager());
        // Accepting adapter.
        generalList.setAdapter(generalAdapter);
        generalList.setOnItemClickListener((parent, view, position, id) -> {
            selectedBuddy = generalAdapter.getBuddy(position);
            checkStoragePermissions();
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

    private void checkStoragePermissions() {
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (hasPermissions(this, PERMISSION)) {
            onPermissionGranted();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION)) {
                // Show an explanation to the user
                new AlertDialog.Builder(SharingActivity.this)
                        .setTitle(R.string.permission_request_title)
                        .setMessage(R.string.share_files_permission_request_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        SharingActivity.this,
                                        new String[]{PERMISSION},
                                        REQUEST_SHARE_FILE
                                );
                            }
                        })
                        .show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{PERMISSION},
                        REQUEST_SHARE_FILE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted();
        } else {
            Snackbar.make(generalList, R.string.share_files_permission_request_message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onPermissionGranted() {
        Logger.log("Opening dialog with buddy (db id): " + selectedBuddy);
        try {
            // Trying to open dialog with this buddy.
            QueryHelper.modifyDialog(databaseLayer, selectedBuddy, true);
            // Open chat dialog for this buddy.
            Intent intent = new Intent(SharingActivity.this, ChatActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Buddy.KEY_STRUCT, selectedBuddy)
                    .putExtra(EXTRA_SHARING_DATA, sharingData);
            startActivity(intent);
            finish();
        } catch (Exception ignored) {
            // Nothing to do in this case.
        }
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
