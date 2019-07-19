package com.tomclaw.mandarin.main.icq;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.CredentialsCheckCallback;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.preferences.PreferenceHelper;

/**
 * Created by Solkin on 28.09.2014.
 */
public class PlainLoginActivity extends ChiefActivity {

    private EditText userIdEditText;
    private EditText userPasswordEditText;
    private AccountRoot accountRoot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountRoot = new IcqAccountRoot();

        setContentView(R.layout.icq_uin_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        View view = findViewById(R.id.register_using_phone_view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(IntroActivity.RESULT_REDIRECT_PHONE_LOGIN);
                finish();
            }
        });

        userIdEditText = (EditText) findViewById(R.id.user_id_field);
        userPasswordEditText = (EditText) findViewById(R.id.user_password_field);

        TextWatcher checkActionTextWatcher = new TextWatcher() {
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
        };

        userIdEditText.addTextChangedListener(checkActionTextWatcher);

        userPasswordEditText.addTextChangedListener(checkActionTextWatcher);
        userPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (isActionVisible()) {
                        checkAccount();
                    }
                    return true;
                }
                return false;
            }
        });

        updateActionVisibility();
    }

    private void updateActionVisibility() {
        invalidateOptionsMenu();
    }

    private boolean isActionVisible() {
        String userId = userIdEditText.getText().toString();
        String password = userPasswordEditText.getText().toString();
        return !(TextUtils.isEmpty(userId) || TextUtils.isEmpty(password));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.login_activity_menu, menu);
        final MenuItem item = menu.findItem(R.id.save_action_menu);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.save_action_menu: {
                checkAccount();
                break;
            }
        }
        return true;
    }

    @Override
    public void updateTheme() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {

    }

    private void checkAccount() {
        String userId = userIdEditText.getText().toString();
        String userPassword = userPasswordEditText.getText().toString();
        // Check for credentials are filed correctly
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.email_or_uin_empty, Toast.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, R.string.password_empty, Toast.LENGTH_LONG).show();
        } else {
            hideKeyboard();
            final ProgressDialog progressDialog = ProgressDialog.show(this, null,
                    getString(R.string.checking_credentials));
            progressDialog.show();
            accountRoot.setContext(this);
            accountRoot.setUserId(userId);
            accountRoot.setUserNick(userId);
            accountRoot.setUserPassword(userPassword);
            accountRoot.checkCredentials(new CredentialsCheckCallback() {

                @Override
                public void onPassed() {
                    MainExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            storeAccountRoot();
                            progressDialog.dismiss();
                        }
                    });
                }

                @Override
                public void onFailed() {
                    MainExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(PlainLoginActivity.this, R.string.invalid_credentials,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private void storeAccountRoot() {
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
            Toast.makeText(this, R.string.account_add_fail, Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(userIdEditText.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(userPasswordEditText.getWindowToken(), 0);
    }
}
