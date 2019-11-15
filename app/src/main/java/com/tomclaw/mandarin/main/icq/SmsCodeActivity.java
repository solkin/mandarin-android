package com.tomclaw.mandarin.main.icq;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.tomclaw.helpers.TimeHelper;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.preferences.PreferenceHelper;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Created by Solkin on 02.10.2014.
 */
public class SmsCodeActivity extends ChiefActivity {

    public static String EXTRA_MSISDN = "msisdn";
    public static String EXTRA_TRANS_ID = "trans_id";
    public static String EXTRA_PHONE_FORMATTED = "phone_formatted";

    private static final long SMS_WAIT_INTERVAL = 60 * 1000;
    private static final int MIN_SMS_CODE_LENGTH = 4;

    private EditText smsCodeField;
    private TextView resendCodeView;

    private RegistrationHelper.RegistrationCallback callback;

    private String transId;
    private String msisdn;

    private SmsTimer timer;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sms_code_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize action bar.
        ActionBar bar = Objects.requireNonNull(getSupportActionBar());
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        msisdn = intent.getStringExtra(EXTRA_MSISDN);
        transId = intent.getStringExtra(EXTRA_TRANS_ID);
        String phoneFormatted = intent.getStringExtra(EXTRA_PHONE_FORMATTED);

        smsCodeField = findViewById(R.id.sms_code_field);
        smsCodeField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateActionVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        smsCodeField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (isActionVisible()) {
                    loginPhone();
                }
                return true;
            }
            return false;
        });

        TextView smsCodeHeader = findViewById(R.id.sms_code_header_view);
        String text = String.format(getResources().getString(R.string.sms_code_header), phoneFormatted);
        smsCodeHeader.setText(Html.fromHtml(text));

        resendCodeView = findViewById(R.id.resend_code_view);
        resendCodeView.setOnClickListener(v -> {
            if (v.isEnabled()) {
                showProgress(R.string.requesting_sms_code);
                RegistrationHelper.validatePhone(msisdn, callback);
            }
        });
        startTimer();

        callback = new RegistrationHelper.RegistrationCallback() {
            @Override
            public void onPhoneNormalized(String msisdn) {
            }

            @Override
            public void onPhoneValidated(final String msisdn, final String transId) {
                MainExecutor.execute(() -> {
                    hideProgress();
                    setTransId(transId);
                    startTimer();
                });
            }

            @Override
            public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime) {
                final IcqAccountRoot accountRoot = new IcqAccountRoot();
                accountRoot.setContext(SmsCodeActivity.this);
                accountRoot.setUserId(login);
                accountRoot.setClientLoginResult(login, tokenA, sessionKey, expiresIn, hostTime);
                MainExecutor.execute(() -> storeAccountRoot(accountRoot));
            }

            @Override
            public void onProtocolError() {
                MainExecutor.execute(() -> onRequestError(R.string.checking_sms_error));
            }

            @Override
            public void onNetworkError() {
                MainExecutor.execute(() -> onRequestError(R.string.checking_sms_error));
            }
        };
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new SmsTimer(resendCodeView);
        timer.start();
    }

    private void updateActionVisibility() {
        invalidateOptionsMenu();
    }

    private boolean isActionVisible() {
        return getSmsCode().length() >= MIN_SMS_CODE_LENGTH;
    }

    @Override
    public void updateTheme() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu, R.menu.sms_code_menu, R.id.sms_code_menu);
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private void inflateMenu(final Menu menu, int menuRes, int menuItem) {
        getMenuInflater().inflate(menuRes, menu);
        final MenuItem item = menu.findItem(menuItem);
        TextView actionView = ((TextView) item.getActionView());
        actionView.setText(actionView.getText().toString().toUpperCase());
        actionView.setOnClickListener(v -> menu.performIdentifierAction(item.getItemId(), 0));

        if (isActionVisible()) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.sms_code_menu: {
                loginPhone();
                break;
            }
        }
        return true;
    }

    /**
     * Check and return entered Sms code
     *
     * @return String - digits phone number
     */
    private String getSmsCode() {
        String smsCode = "";
        if (!TextUtils.isEmpty(smsCodeField.getText())) {
            smsCode = String.valueOf(smsCodeField.getText());
        }
        return smsCode;
    }

    private void loginPhone() {
        // Now, take the rest, hide keyboard...
        hideKeyboard();
        // ... and wait for account activation.
        loginPhone(msisdn, transId, getSmsCode());
    }

    private void loginPhone(final String msisdn, final String transId, final String smsCode) {
        showProgress(R.string.checking_sms_code);
        RegistrationHelper.loginPhone(msisdn, transId, smsCode, callback);
    }

    @SuppressWarnings("SameParameterValue")
    private void onRequestError(int message) {
        hideProgress();
        showError(message);
    }

    private void showError(int message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.phone_auth_error)
                .setMessage(message)
                .setCancelable(true)
                .setNeutralButton(R.string.got_it, null)
                .show();
    }

    private void storeAccountRoot(IcqAccountRoot accountRoot) {
        hideProgress();
        try {
            // Store account into database.
            ContentResolver contentResolver = getContentResolver();
            DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
            int accountDbId = QueryHelper.insertAccount(this, databaseLayer, accountRoot);
            getServiceInteraction().holdAccount(accountDbId);
            // Connect account right now!
            int connectStatus = StatusUtil.getDefaultOnlineStatus(accountRoot.getAccountType());
            getServiceInteraction().updateAccountStatusIndex(
                    accountRoot.getAccountType(), accountRoot.getUserId(), connectStatus);
            // We are started as start helper and now
            // mission is complete, switch off the flag...
            PreferenceHelper.setShowStartHelper(this, false);
            // ... and now will go to the dialogs activity.
            setResult(RESULT_OK);
            finish();
        } catch (Throwable ignored) {
            showError(R.string.account_add_fail);
        }
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(smsCodeField.getWindowToken(), 0);
    }

    private void showProgress(int message) {
        progressDialog = ProgressDialog.show(this, null, getString(message));
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public static class SmsTimer extends CountDownTimer {

        private TimeHelper timeHelper;
        private WeakReference<TextView> weakResendCode;

        SmsTimer(TextView resendCodeView) {
            super(SMS_WAIT_INTERVAL, 1000);
            timeHelper = new TimeHelper(resendCodeView.getContext());
            weakResendCode = new WeakReference<>(resendCodeView);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            TextView resendCodeView = weakResendCode.get();
            if (resendCodeView != null) {
                if (resendCodeView.isEnabled()) {
                    resendCodeView.setEnabled(false);
                }
                String time = timeHelper.getTime(millisUntilFinished);
                setUnderlinedString(resendCodeView.getResources().getString(R.string.resend_code_time, time));
            }
        }

        @Override
        public void onFinish() {
            TextView resendCodeView = weakResendCode.get();
            if (resendCodeView != null) {
                resendCodeView.setEnabled(true);
                setUnderlinedString(resendCodeView.getResources().getString(R.string.resend_code));
            }
        }

        private void setUnderlinedString(String text) {
            TextView resendCodeView = weakResendCode.get();
            if (resendCodeView != null) {
                SpannableString content = new SpannableString(text);
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                resendCodeView.setText(content);
            }
        }
    }
}
