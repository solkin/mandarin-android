package com.tomclaw.mandarin.core.accounts;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.AccountAddActivity;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/18/13
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {
    public static final String ACCOUNT_TYPE = "com.tomclaw.mandarin";
    public static String AUTHTOKEN_TYPE="accounts.token";
    private Context mContext;

    public AccountAuthenticator(Context context) {
        super(context);
        this.mContext = context;
        Log.d("ACCOUNT", "AccountAuthenticator");
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {

        Log.d("ACCOUNT", "addAccount");

        final Bundle result;
        final Intent intent;

        intent = new Intent(mContext, AccountAddActivity.class);
        intent.putExtra(AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra("acc.type", accountType);
        intent.putExtra(AccountAddActivity.CLASS_NAME_EXTRA, IcqAccountRoot.class.getName());
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        result = new Bundle();
        result.putParcelable(AccountManager.KEY_INTENT, intent);

        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAuthTokenLabel(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
