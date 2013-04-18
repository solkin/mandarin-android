package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
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
        String className = getIntent().getStringExtra("ClassName");
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
        setContentView(R.layout.account_add);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
                    if(accountRoot != null) {
                    accountRoot.setUserId(userId);
                    accountRoot.setUserNick(userId);
                    accountRoot.setUserPassword(userPassword);
                    getServiceInteraction().addAccount(accountRoot);
                    setResult(AccountsActivity.ADDING_ACTIVITY_RESULT_CODE);
                    finish();
                    } else {
                        Toast.makeText(AccountAddActivity.this, R.string.account_add_fail, Toast.LENGTH_LONG).show();
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                /*IcqAccountRoot account = new IcqAccountRoot() {
                    @Override
                    public int getServiceIcon() {
                        return 0;
                    }
                };
                account.setUserId(login);
                account.setUserPassword(password);*/
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCoreServiceDown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
