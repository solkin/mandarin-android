package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.AccountAlreadyExistsException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.MainActivity;

/**
 * Created by Solkin on 02.10.2014.
 */
public class SmsCodeActivity extends ChiefActivity {

    public static String EXTRA_MSISDN = "msisdn";
    public static String EXTRA_TRANS_ID = "trans_id";

    EditText smsCodeField;

    RegistrationHelper.RegistrationCallback callback;

    String transId;
    String msisdn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sms_code_activity);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        Intent intent = getIntent();
        msisdn = intent.getStringExtra(EXTRA_MSISDN);
        transId = intent.getStringExtra(EXTRA_TRANS_ID);

        smsCodeField = (EditText) findViewById(R.id.sms_code_field);

        callback = new RegistrationHelper.RegistrationCallback() {
            @Override
            public void onPhoneNormalized(String msisdn) {
            }

            @Override
            public void onPhoneValidated(final String msisdn, final String transId) {
            }

            @Override
            public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime) {
                final IcqAccountRoot accountRoot = new IcqAccountRoot();
                accountRoot.setContext(SmsCodeActivity.this);
                accountRoot.setUserId(login);
                accountRoot.setClientLoginResult(login, tokenA, sessionKey, expiresIn, hostTime);
                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        storeAccountRoot(accountRoot);
                    }
                });
            }

            @Override
            public void onProtocolError() {
                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onRequestError();
                    }
                });
            }

            @Override
            public void onNetworkError() {
                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onRequestError();
                    }
                });
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu, R.menu.sms_code_menu, R.id.sms_code_menu);
        return true;
    }

    private void inflateMenu(final Menu menu, int menuRes, int menuItem) {
        getMenuInflater().inflate(menuRes, menu);
        final MenuItem item = menu.findItem(menuItem);
        TextView actionView = ((TextView) item.getActionView());
        actionView.setText(actionView.getText().toString().toUpperCase());
        actionView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(item.getItemId(), 0);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.sms_code_menu: {
                String smsCode = smsCodeField.getText().toString();
                loginPhone(msisdn, transId, smsCode);
                break;
            }
        }
        return true;
    }

    private void loginPhone(final String msisdn, final String transId, final String smsCode) {
        RegistrationHelper.loginPhone(msisdn, transId, smsCode, callback);
    }

    private void onRequestError() {
        Toast.makeText(this, "Error. Try again.", Toast.LENGTH_SHORT).show();
    }

    private void storeAccountRoot(IcqAccountRoot accountRoot) {
        try {
            // Store account into database.
            int accountDbId = QueryHelper.insertAccount(this, accountRoot);
            getServiceInteraction().holdAccount(accountDbId);
            // Connect account right now!
            int connectStatus = StatusUtil.getDefaultOnlineStatus(accountRoot.getAccountType());
            getServiceInteraction().updateAccountStatusIndex(
                    accountRoot.getAccountType(), accountRoot.getUserId(), connectStatus);
            //if (isStartHelper()) {
            // We are started as start helper and now
            // mission is complete, switch off the flag...
            PreferenceHelper.setShowStartHelper(this, false);
            // ... and now will go to the dialogs activity.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            //}
            finish();
        } catch (AccountAlreadyExistsException ignored) {
            Toast.makeText(this, R.string.account_already_exists, Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            Toast.makeText(this, R.string.account_add_fail, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }
}
