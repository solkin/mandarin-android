package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.MainActivity;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 4/17/13
 * Time: 4:07 PM
 */
public class IntroActivity extends ChiefActivity implements ChiefActivity.CoreServiceListener {

    public static final String EXTRA_START_HELPER = "start_helper";
    public static final int RESULT_LOGIN_COMPLETED = 1;
    public static final int RESULT_REDIRECT_PHONE_LOGIN = 2;

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
        bar.setTitle(R.string.account);
        bar.setDisplayHomeAsUpEnabled(!isStartHelper());
        // Initialize add account activity.
        setContentView(R.layout.icq_intro);

        findViewById(R.id.phone_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPhoneLogin();
            }
        });
        findViewById(R.id.uin_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlainLogin();
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

    private void startPhoneLogin() {
        startActivityForResult(new Intent(getBaseContext(), PhoneLoginActivity.class), RESULT_LOGIN_COMPLETED);
    }

    private void startPlainLogin() {
        startActivityForResult(new Intent(getBaseContext(), PlainLoginActivity.class), RESULT_LOGIN_COMPLETED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOGIN_COMPLETED) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (resultCode == RESULT_REDIRECT_PHONE_LOGIN) {
                startPhoneLogin();
            }
        }
    }
}
