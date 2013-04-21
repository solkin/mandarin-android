package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 4/17/13
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountAddActivity extends ChiefActivity {

    public static final String CLASS_NAME_EXTRA = "ClassName";
    private Class<? extends AccountRoot> accountRootClass;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain class name extra to setup AccountRoot type.
        String className = getIntent().getStringExtra(CLASS_NAME_EXTRA);
        Log.d(Settings.LOG_TAG, "AccountAddActivity start for " + className);
        try {
            accountRootClass = Class.forName(className).asSubclass(AccountRoot.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.account_add_menu, menu);
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.accounts);
        // Initialize accounts list
        setContentView(R.layout.account_add);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent mainActivityIntent = new Intent(this, AccountsActivity.class);
                startActivity(mainActivityIntent);
                return true;
            case R.id.ok_account_menu:
                String userId = ((EditText) findViewById(R.id.user_id_field)).getText().toString();
                String userPassword = ((EditText) findViewById(R.id.user_password_field)).getText().toString();
                if (TextUtils.isEmpty(userId)) {
                    Toast.makeText(AccountAddActivity.this, R.string.user_id_empty, Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(userPassword)) {
                    Toast.makeText(AccountAddActivity.this, R.string.user_password_empty, Toast.LENGTH_LONG).show();
                } else {
                    try {
                        AccountRoot accountRoot = accountRootClass.newInstance();
                        if (accountRoot == null) {
                            throw new Throwable();
                        } else {
                            accountRoot.setUserId(userId);
                            accountRoot.setUserNick(userId);
                            accountRoot.setUserPassword(userPassword);
                            getServiceInteraction().addAccount(accountRoot);
                            setResult(AccountsActivity.ADDING_ACTIVITY_RESULT_CODE);
                            finish();
                        }
                    } catch (Throwable e) {
                        Toast.makeText(AccountAddActivity.this, R.string.account_add_fail, Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
