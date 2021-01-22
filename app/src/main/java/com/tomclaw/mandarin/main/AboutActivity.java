package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.preferences.PreferenceHelper;

import com.tomclaw.mandarin.util.MetricsManager;

/**
 * Created by solkin on 01/03/14.
 */
public class AboutActivity extends AppCompatActivity {

    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String MARKET_DEVELOPER_URI = "market://search?q=";
    private static final String GOOGLE_PLAY_DETAILS_URI = "https://play.google.com/store/apps/details?id=";
    private static final String GOOGLE_PLAY_DEVELOPER_URI = "https://play.google.com/store/apps/search?q=";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int themeRes = PreferenceHelper.getThemeRes(this);
        setTheme(themeRes);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView appVersionView = findViewById(R.id.app_version);
        PackageManager manager = getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            appVersionView.setText(getString(R.string.app_version, info.versionName, info.versionCode));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        findViewById(R.id.rate_application).setOnClickListener(v -> rateApplication());

        findViewById(R.id.all_projects).setOnClickListener(v -> allProjects());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        MetricsManager.trackEvent("Open about");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
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
