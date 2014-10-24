package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;
import com.tomclaw.mandarin.util.PhoneNumberFormattingTextWatcher;

/**
 * Created by Solkin on 28.09.2014.
 */
public class PhoneLoginActivity extends Activity {

    private static final int REQUEST_CODE_COUNTRY = 1;
    private static final int REQUEST_SMS_NUMBER = 2;

    private static final int MIN_PHONE_BODY_LENGTH = 10;

    private TextView countryCodeView;
    private TextView countryNameView;
    private EditText phoneNumberField;

    private RegistrationHelper.RegistrationCallback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.icq_phone_login);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setIcon(R.drawable.ic_ab_logo);

        Country country;
        try {
            country = CountriesProvider.getInstance().getCountryByCurrentLocale(this, getString(R.string.default_locale));
        } catch (CountriesProvider.CountryNotFoundException ignored) {
            // This is rather strange situation. No current or event default locale?
            country = null;
        }

        View.OnClickListener showCountryListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(PhoneLoginActivity.this, CountryCodeActivity.class),
                        REQUEST_CODE_COUNTRY);
            }
        };

        countryCodeView = (TextView) findViewById(R.id.country_code_view);
        countryCodeView.setOnClickListener(showCountryListener);

        countryNameView = (TextView) findViewById(R.id.country_name_view);
        countryNameView.setOnClickListener(showCountryListener);

        phoneNumberField = (EditText) findViewById(R.id.phone_number_field);
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
        phoneNumberField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_NEXT) {
                    if(isActionVisible()) {
                        requestSms();
                    }
                    return true;
                }
                return false;
            }
        });

        TextView privacyPolicyView = (TextView) findViewById(R.id.privacy_policy_view);
        privacyPolicyView.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicyView.setFocusable(true);

        updateCountryViews(country);

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
        updateActionVisibility();
    }

    private void updateActionVisibility() {
        invalidateOptionsMenu();
    }

    private boolean isActionVisible() {
        return getPhoneNumber().length() >= MIN_PHONE_BODY_LENGTH;
    }

    private void updateCountryViews(Country country) {
        countryCodeView.setText("+" + country.getCode());
        countryNameView.setText(country.getName() + " (+" + country.getCode() + ")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu, R.menu.phone_enter_menu, R.id.phone_enter_menu);
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
     * @return String - country code digits
     */
    private String getCountryCode() {
        String countryCode = "";
        if(!TextUtils.isEmpty(countryCodeView.getText())) {
            countryCode = String.valueOf(countryCodeView.getText());
            countryCode = countryCode.replace("+", "");
        }
        return countryCode;
    }

    /**
     * Convert keypad letters to digits and strip separators
     * @return String - digits phone number
     */
    private String getPhoneNumber() {
        String phoneNumber = "";
        if(!TextUtils.isEmpty(phoneNumberField.getText())) {
            phoneNumber = String.valueOf(phoneNumberField.getText());
            phoneNumber = PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber);
            phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
        }
        return phoneNumber;
    }

    private void requestSms(final String countryCode, final String phoneNumber) {
        RegistrationHelper.normalizePhone(countryCode, phoneNumber, callback);
    }

    private void onSmsSent(String msisdn, String transId) {
        startActivityForResult(new Intent(this, SmsCodeActivity.class)
                .putExtra(SmsCodeActivity.EXTRA_MSISDN, msisdn)
                .putExtra(SmsCodeActivity.EXTRA_TRANS_ID, transId), REQUEST_SMS_NUMBER);
    }

    private void onRequestError() {
        Toast.makeText(this, "Error. Try again.", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phoneNumberField.getWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
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
