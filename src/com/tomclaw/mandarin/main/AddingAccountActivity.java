package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 4/17/13
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddingAccountActivity extends ChiefActivity{

    EditText editLogin;
    EditText editPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adding_account);
        editLogin = (EditText) findViewById(R.id.enter_login);
        editPassword = (EditText) findViewById(R.id.enter_password);
        Button submitButton = (Button) findViewById(R.id.login_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = editLogin.getText().toString();
                if (login == ""){
                    Toast.makeText(AddingAccountActivity.this, "Enter login please", Toast.LENGTH_LONG).show();
                }
                String password = editPassword.getText().toString();
                if (login == ""){
                    Toast.makeText(AddingAccountActivity.this, "Enter password please", Toast.LENGTH_LONG).show();
                }
                IcqAccountRoot account = new IcqAccountRoot() {
                    @Override
                    public int getServiceIcon() {
                        return 0;
                    }
                } ;
                account.setUserId(login);
                account.setUserPassword(password);
                try {
                    getServiceInteraction().addAccount(account);
                    setResult(AccountsActivity.ADDING_ACTIVITY_RESULT_CODE);
                    finish();
                }   catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        });
    }



    @Override
    public void onCoreServiceReady() {
        //To change body of implemented methods use File | Settings | File Templates.
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
