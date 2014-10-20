package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.exceptions.AccountAlreadyExistsException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.CredentialsCheckCallback;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.MainActivity;

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

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        userIdEditText = (EditText) findViewById(R.id.user_id_field);
        userPasswordEditText = (EditText) findViewById(R.id.user_password_field);
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(userIdEditText.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(userPasswordEditText.getWindowToken(), 0);
    }
}
