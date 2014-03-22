package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.AccountAlreadyExistsException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.CredentialsCheckCallback;
import com.tomclaw.mandarin.im.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 4/17/13
 * Time: 4:07 PM
 */
public class AccountAddActivity extends ChiefActivity {

    public static final String EXTRA_CLASS_NAME = "class_name";
    public static final String EXTRA_START_HELPER = "start_helper";
    private AccountRoot accountRoot;
    private EditText userIdEditText;
    private EditText userPasswordEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain class name extra to setup AccountRoot type.
        String className = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        Log.d(Settings.LOG_TAG, "AccountAddActivity start for " + className);
        try {
            Class<? extends AccountRoot> accountRootClass = Class.forName(className).asSubclass(AccountRoot.class);
            accountRoot = accountRootClass.newInstance();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_add_menu, menu);
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.account);
        bar.setDisplayHomeAsUpEnabled(!isStartHelper());
        // Initialize add account activity.
        setContentView(accountRoot.getAccountLayout());
        userIdEditText = ((EditText) findViewById(R.id.user_id_field));
        userPasswordEditText = ((EditText) findViewById(R.id.user_password_field));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(Settings.LOG_TAG, "onOptionsItemSelected: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.ok_account_menu: {
                String userId = userIdEditText.getText().toString();
                String userPassword = userPasswordEditText.getText().toString();
                // Check for credentials are filed correctly
                if (TextUtils.isEmpty(userId)) {
                    Toast.makeText(AccountAddActivity.this, R.string.user_id_empty, Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(userPassword)) {
                    Toast.makeText(AccountAddActivity.this, R.string.user_password_empty, Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(AccountAddActivity.this, R.string.invalid_credentials,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
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
            if (isStartHelper()) {
                // We are started as start helper and now
                // mission is complete, switch off the flag...
                PreferenceHelper.setShowStartHelper(this, false);
                // ... and now will go to the dialogs activity.
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else {
                // Creating signal intent.
                setResult(RESULT_OK);
            }
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
