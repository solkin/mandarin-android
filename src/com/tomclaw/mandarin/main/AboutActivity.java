package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.tomclaw.mandarin.R;

/**
 * Created by solkin on 01/03/14.
 */
public class AboutActivity extends ChiefActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_activity);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
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
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
