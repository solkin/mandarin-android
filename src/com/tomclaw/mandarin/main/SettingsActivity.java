package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MusicStateReceiver;
import com.tomclaw.mandarin.core.PreferenceHelper;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 9/30/13
 * Time: 7:37 PM
 */
public class SettingsActivity extends Activity {

    private SharedPreferences preferences;
    private OnSettingsChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        listener = new OnSettingsChangedListener();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(listener);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(R.drawable.ic_ab_logo);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource.
            addPreferencesFromResource(R.xml.preferences);
        }
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
                startActivity(intent);
            }
        }
    }
}
