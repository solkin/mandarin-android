package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.ChiefActivity;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 4/17/13
 * Time: 4:07 PM
 */
public class IcqIntroActivity extends ChiefActivity implements ChiefActivity.CoreServiceListener {

    public static final String EXTRA_START_HELPER = "start_helper";
    private EditText userIdEditText;
    private EditText userPasswordEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We want to receive service state notifications.
        addCoreServiceListener(this);
        getActionBar().hide();
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.account);
        bar.setDisplayHomeAsUpEnabled(!isStartHelper());
        // Initialize add account activity.
        setContentView(R.layout.icq_intro);
        userIdEditText = ((EditText) findViewById(R.id.user_id_field));
        userPasswordEditText = ((EditText) findViewById(R.id.user_password_field));

        findViewById(R.id.phone_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), IcqPhoneLoginActivity.class));
            }
        });
        findViewById(R.id.uin_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), IcqUinLoginActivity.class));
            }
        });
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    private boolean isStartHelper() {
        return getIntent().getBooleanExtra(EXTRA_START_HELPER, false);
    }
}
