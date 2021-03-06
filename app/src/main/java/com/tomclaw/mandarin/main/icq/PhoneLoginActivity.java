package com.tomclaw.mandarin.main.icq;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;
import com.tomclaw.mandarin.util.PhoneNumberFormattingTextWatcher;

import java.util.Objects;

import com.tomclaw.mandarin.util.MetricsManager;

/**
 * Created by Solkin on 28.09.2014.
 */
public class PhoneLoginActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_COUNTRY = 1;
    private static final int REQUEST_SMS_NUMBER = 2;

    private static final int MIN_PHONE_BODY_LENGTH = 6;

    private TextView countryCodeView;
    private TextView countryNameView;
    private EditText phoneNumberField;

    private ProgressDialog progressDialog;

    private RegistrationHelper.RegistrationCallback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.icq_phone_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize action bar.
        ActionBar bar = Objects.requireNonNull(getSupportActionBar());
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);

        Country country;
        try {
            country = CountriesProvider.getInstance().getCountryByCurrentLocale(this, getString(R.string.default_locale));
        } catch (CountriesProvider.CountryNotFoundException ignored) {
            // This is rather strange situation. No current or event default locale?
            country = new Country("USA", 1, "US");
        }

        View.OnClickListener showCountryListener = v -> startActivityForResult(new Intent(PhoneLoginActivity.this, CountryCodeActivity.class),
                REQUEST_CODE_COUNTRY);

        countryCodeView = findViewById(R.id.country_code_view);
        countryCodeView.setOnClickListener(showCountryListener);

        countryNameView = findViewById(R.id.country_name_view);
        countryNameView.setOnClickListener(showCountryListener);

        phoneNumberField = findViewById(R.id.phone_number_field);
        phoneNumberField.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        phoneNumberField.addTextChangedListener(new TextWatcher() {
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
        phoneNumberField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (isActionVisible()) {
                    requestSms();
                }
                return true;
            }
            return false;
        });

        View view = findViewById(R.id.phone_number_faq_view);
        view.setOnClickListener(v -> showPhoneNumberFaq());

        TextView privacyPolicyView = findViewById(R.id.privacy_policy_view);
        privacyPolicyView.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicyView.setFocusable(true);

        updateCountryViews(country);

        callback = new RegistrationHelper.RegistrationCallback() {

            @Override
            public void onPhoneValidated(final String phoneNumber, final int codeLength, final String sessionId) {
                MainExecutor.execute(() -> onSmsSent(phoneNumber, codeLength, sessionId));
            }

            @Override
            public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime) {
            }

            @Override
            public void onProtocolError() {
                MainExecutor.execute(() -> onRequestError(R.string.checking_phone_protocol_error));
            }

            @Override
            public void onNetworkError() {
                MainExecutor.execute(() -> onRequestError(R.string.phone_auth_network_error));
            }
        };
        updateActionVisibility();
        MetricsManager.trackEvent("Open phone login");
    }

    private void updateActionVisibility() {
        invalidateOptionsMenu();
    }

    private boolean isActionVisible() {
        return getPhoneNumber().length() >= MIN_PHONE_BODY_LENGTH;
    }

    @SuppressLint("SetTextI18n")
    private void updateCountryViews(Country country) {
        countryCodeView.setText("+" + country.getCode());
        countryNameView.setText(country.getName() + " (+" + country.getCode() + ")");
    }

    private void showPhoneNumberFaq() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.phone_number_faq_message)
                .setCancelable(true)
                .setNeutralButton(R.string.got_it, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu, R.menu.phone_enter_menu, R.id.phone_enter_menu);
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private void inflateMenu(final Menu menu, int menuRes, int menuItem) {
        getMenuInflater().inflate(menuRes, menu);
        final MenuItem item = menu.findItem(menuItem);
        TextView actionView = ((TextView) item.getActionView());
        actionView.setText(actionView.getText().toString().toUpperCase());
        actionView.setOnClickListener(v -> menu.performIdentifierAction(item.getItemId(), 0));

        item.setVisible(isActionVisible());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.phone_enter_menu: {
                requestSms();
                break;
            }
        }
        return true;
    }

    private void requestSms() {
        // Now, take the rest, hide keyboard...
        hideKeyboard();
        // ... and wait for Sms code.
        requestSms(getCountryCode(), getPhoneNumber());
    }

    /**
     * Returns only country code digits without "+"
     *
     * @return String - country code digits
     */
    private String getCountryCode() {
        String countryCode = "";
        if (!TextUtils.isEmpty(countryCodeView.getText())) {
            countryCode = String.valueOf(countryCodeView.getText());
            countryCode = countryCode.replace("+", "");
        }
        return countryCode;
    }

    /**
     * Convert keypad letters to digits and strip separators
     *
     * @return String - digits phone number
     */
    private String getPhoneNumber() {
        String phoneNumber = "";
        if (!TextUtils.isEmpty(phoneNumberField.getText())) {
            phoneNumber = String.valueOf(phoneNumberField.getText());
            phoneNumber = PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber);
            phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
        }
        return phoneNumber;
    }

    private String getPhoneFormatted() {
        return TextUtils.concat(countryCodeView.getText(), " ", phoneNumberField.getText()).toString();
    }

    private void requestSms(final String countryCode, final String phoneNumber) {
        showProgress();
        RegistrationHelper.normalizePhone(countryCode + phoneNumber, callback);
    }

    private void onSmsSent(String phoneNumber, int codeLength, String sessionId) {
        hideProgress();
        startActivityForResult(
                new Intent(this, SmsCodeActivity.class)
                        .putExtra(SmsCodeActivity.EXTRA_PHONE_NUMBER, phoneNumber)
                        .putExtra(SmsCodeActivity.EXTRA_CODE_LENGTH, codeLength)
                        .putExtra(SmsCodeActivity.EXTRA_SESSION_ID, sessionId)
                        .putExtra(SmsCodeActivity.EXTRA_PHONE_FORMATTED, getPhoneFormatted()),
                REQUEST_SMS_NUMBER
        );
    }

    private void onRequestError(int message) {
        hideProgress();
        new AlertDialog.Builder(this)
                .setTitle(R.string.phone_auth_error)
                .setMessage(message)
                .setCancelable(true)
                .setNeutralButton(R.string.got_it, null)
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phoneNumberField.getWindowToken(), 0);
    }

    private void showProgress() {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.checking_phone_number));
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_COUNTRY) {
                String countryShortName = data.getStringExtra(CountryCodeActivity.EXTRA_COUNTRY_SHORT_NAME);
                if (!TextUtils.isEmpty(countryShortName)) {
                    try {
                        Country country = CountriesProvider.getInstance().getCountryByLocale(
                                this, countryShortName, countryShortName);
                        updateCountryViews(country);
                    } catch (CountriesProvider.CountryNotFoundException ignored) {
                        // No any case. This code is coming from this country provider list.
                    }
                }
            } else if (requestCode == REQUEST_SMS_NUMBER) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}
