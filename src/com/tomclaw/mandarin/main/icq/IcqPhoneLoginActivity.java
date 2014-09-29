package com.tomclaw.mandarin.main.icq;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.AccountAlreadyExistsException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.MainActivity;

/**
 * Created by Solkin on 28.09.2014.
 */
public class IcqPhoneLoginActivity extends ChiefActivity {

    ViewSwitcher loginViewSwitcher;

    EditText countryCodeField;
    EditText phoneNumberField;
    EditText smsCodeField;

    String transId;
    String msisdn;

    RegistrationHelper.RegistrationCallback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.icq_phone_login);

        loginViewSwitcher = (ViewSwitcher) findViewById(R.id.phone_login_view_switcher);

        countryCodeField = (EditText) findViewById(R.id.country_code_field);
        phoneNumberField = (EditText) findViewById(R.id.phone_number_field);
        smsCodeField = (EditText) findViewById(R.id.sms_code_field);

        phoneNumberField.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        findViewById(R.id.validate_phone_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String countryCode = countryCodeField.getText().toString();
                String phoneNumber = phoneNumberField.getText().toString();
                requestSms(countryCode, phoneNumber);
            }
        });
        findViewById(R.id.login_phone_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String smsCode = smsCodeField.getText().toString();
                loginPhone(msisdn, transId, smsCode);
            }
        });

        callback = new RegistrationHelper.RegistrationCallback() {
            @Override
            public void onPhoneNormalized(String msisdn) {
                RegistrationHelper.validatePhone(msisdn, callback);
            }

            @Override
            public void onPhoneValidated(final String msisdn, final String transId) {
                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onSmsSent(msisdn, transId);
                    }
                });
            }

            @Override
            public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime) {
                final IcqAccountRoot accountRoot = new IcqAccountRoot();
                accountRoot.setContext(IcqPhoneLoginActivity.this);
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

    private void requestSms(final String countryCode, final String phoneNumber) {
        RegistrationHelper.normalizePhone(countryCode, phoneNumber, callback);
    }

    private void onSmsSent(String msisdn, String transId) {
        this.msisdn = msisdn;
        this.transId = transId;
        loginViewSwitcher.showNext();
    }

    private void onRequestError() {
        Toast.makeText(this, "Error. Try again.", Toast.LENGTH_SHORT).show();
    }

    private void loginPhone(final String msisdn, final String transId, final String smsCode) {
        RegistrationHelper.loginPhone(msisdn, transId, smsCode, callback);
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
