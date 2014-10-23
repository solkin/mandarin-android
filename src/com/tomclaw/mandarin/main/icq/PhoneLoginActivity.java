package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.im.icq.RegistrationHelper;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;

import java.util.Locale;

/**
 * Created by Solkin on 28.09.2014.
 */
public class PhoneLoginActivity extends Activity {

    private static int REQUEST_CODE_COUNTRY = 1;

    private TextView countryCodeView;
    private TextView countryNameView;
    private EditText phoneNumberField;

    private TextView actionView;

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
                checkActionVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

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
    }

    private void checkActionVisibility() {
        if(actionView != null) {
            String phoneNumber = getPhoneNumber();
            if (phoneNumber.length() >= 6) {
                actionView.setVisibility(View.VISIBLE);
            } else {
                actionView.setVisibility(View.INVISIBLE);
            }
        }
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
        actionView = ((TextView) item.getActionView());
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
            case R.id.phone_enter_menu: {
                requestSms(getCountryCode(), getPhoneNumber());
                break;
            }
        }
        return true;
    }

    private String getCountryCode() {
        return countryCodeView.getText().toString().substring(1);
    }

    private String getPhoneNumber() {
        return phoneNumberField.getText().toString().replace(" ", "");
    }

    private void requestSms(final String countryCode, final String phoneNumber) {
        RegistrationHelper.normalizePhone(countryCode, phoneNumber, callback);
    }

    private void onSmsSent(String msisdn, String transId) {
        startActivity(new Intent(this, SmsCodeActivity.class)
                .putExtra(SmsCodeActivity.EXTRA_MSISDN, msisdn)
                .putExtra(SmsCodeActivity.EXTRA_TRANS_ID, transId));
    }

    private void onRequestError() {
        Toast.makeText(this, "Error. Try again.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_COUNTRY && resultCode == RESULT_OK) {
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
        }
    }
}
