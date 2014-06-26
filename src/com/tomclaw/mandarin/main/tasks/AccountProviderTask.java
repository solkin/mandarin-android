package com.tomclaw.mandarin.main.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.adapters.AccountsSelectorAdapter;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created by Solkin on 13.06.2014.
 */
public class AccountProviderTask extends WeakObjectTask<Activity> {

    private AccountProviderCallback callback;
    private boolean isShowDialog = false;

    public AccountProviderTask(Activity object, AccountProviderCallback callback) {
        super(object);
        this.callback = callback;
    }

    @Override
    public void executeBackground() throws Throwable {
        Activity context = getWeakObject();
        if(context != null) {
            Cursor cursor = null;
            try {
                cursor = QueryHelper.getActiveAccountsCount(context.getContentResolver());
                if(cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst()) {
                    callback.onNoActiveAccounts();
                    isShowDialog = false;
                } else if(cursor.getCount() == 1) {
                    int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                    callback.onAccountSelected(accountDbId);
                    isShowDialog = false;
                } else {
                    isShowDialog = true;
                }
            } finally {
                if(cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onPostExecuteMain() {
        Activity context = getWeakObject();
        if(context != null && isShowDialog) {
            View view = LayoutInflater.from(context).inflate(R.layout.account_selector_dialog, null);

            ListView listView = (ListView) view.findViewById(R.id.accounts_list_view);
            AccountsSelectorAdapter adapter = new AccountsSelectorAdapter(context, context.getLoaderManager());
            listView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.select_account_title)
                    .setView(view)
                    .create();
            dialog.show();
        }
    }

    public interface AccountProviderCallback {

        public void onAccountSelected(int accountDbId);
        public void onNoActiveAccounts();
    }
}
