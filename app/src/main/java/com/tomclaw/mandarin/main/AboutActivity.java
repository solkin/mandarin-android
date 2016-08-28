package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created by solkin on 01/03/14.
 */
public class AboutActivity extends AppCompatActivity {

    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String MARKET_DEVELOPER_URI = "market://search?q=";
    private static final String GOOGLE_PLAY_DETAILS_URI = "http://play.google.com/store/apps/details?id=";
    private static final String GOOGLE_PLAY_DEVELOPER_URI = "http://play.google.com/store/apps/search?q=";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView appVersionView = (TextView) findViewById(R.id.app_version);
        PackageManager manager = getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            appVersionView.setText(getString(R.string.app_version, info.versionName, info.versionCode));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        findViewById(R.id.rate_application).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateApplication();
            }
        });

        findViewById(R.id.all_projects).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allProjects();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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

    private void rateApplication() {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_DETAILS_URI + appPackageName)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_DETAILS_URI + appPackageName)));
        }
    }

    private void allProjects() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_DEVELOPER_URI + Settings.DEVELOPER_NAME)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_DEVELOPER_URI + Settings.DEVELOPER_NAME)));
        }
    }
}
