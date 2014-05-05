package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class AccountInfoActivity extends BuddyInfoActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.account_shutdown:
                try {
                    // Trying to disconnect account.
                    getServiceInteraction().updateAccountStatusIndex(
                            getAccountType(), getBuddyId(), StatusUtil.STATUS_OFFLINE);
                    finish();
                } catch (RemoteException ignored) {
                    // Heh... Nothing to do in this case.
                    Toast.makeText(this, R.string.unable_to_shutdown_account, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.account_remove:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.remove_account_title);
                builder.setMessage(R.string.remove_account_text);
                builder.setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Collection<Integer> selectedAccounts = Collections.singleton(getAccountDbId());
                        AccountsRemoveTask task = new AccountsRemoveTask(AccountInfoActivity.this, selectedAccounts) {

                            @Override
                            public void onSuccessMain() {
                                ChiefActivity chiefActivity = weakChiefActivity.get();
                                if (chiefActivity != null) {
                                    chiefActivity.finish();
                                }
                            }
                        };
                        TaskExecutor.getInstance().execute(task);
                    }
                });
                builder.setNegativeButton(R.string.do_not_remove, null);
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settings.LOG_TAG, "AccountInfoActivity onCreate");

        // Preparing for action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            bar.setTitle(R.string.account_info);
        }
    }

    @Override
    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_account_info, Toast.LENGTH_SHORT).show();
    }
}
