package com.tomclaw.mandarin.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BootCompletedReceiver;
import com.tomclaw.mandarin.core.MusicStateReceiver;
import com.tomclaw.mandarin.core.PreferenceHelper;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 9/30/13
 * Time: 7:37 PM
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private OnSettingsChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listener = new OnSettingsChangedListener();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(listener);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
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

    public class OnSettingsChangedListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Context context = SettingsActivity.this;
            // Checking for preference changed.
            if (TextUtils.equals(key, getString(R.string.pref_music_auto_status))) {
                // If music is already active and setting is became on, we must notify user.
                if (MusicStateReceiver.isMusicActive(context)) {
                    if (PreferenceHelper.isMusicAutoStatus(context)) {
                        Toast.makeText(context, R.string.update_after_track_switch, Toast.LENGTH_SHORT).show();
                    } else {
                        MusicStateReceiver.sendEventToService(context);
                    }
                }
            } else if (TextUtils.equals(key, getString(R.string.pref_dark_theme))) {
                Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
            } else if (TextUtils.equals(key, getString(R.string.pref_autorun))) {
                // Enable or disable application start after boot.
                int state = PreferenceHelper.isAutorun(context) ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                ComponentName component = new ComponentName(context, BootCompletedReceiver.class);
                getPackageManager().setComponentEnabledSetting(component, state,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }
}
