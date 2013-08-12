package com.tomclaw.mandarin.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.vk.VkAccountAddActivity;
import com.tomclaw.mandarin.im.vk.VkAccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 8/8/13
 * Time: 6:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountTypeSelectionDialog extends DialogFragment{

    final static CharSequence[] items = {"Icq", "VK"};
    final static String[] classNames = {IcqAccountRoot.class.getName(), VkAccountRoot.class.getName()};
    final static Class[] addActivityClasses = {AccountAddActivity.class, VkAccountAddActivity.class};
    final static int[] requestCodes = {AccountsActivity.ADD_ACTIVITY_REQUEST_CODE, AccountsActivity.VK_ADD_ACTIVITY_REQUEST_CODE};

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select account type");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                Intent accountAddIntent = new Intent(getActivity(), addActivityClasses[item]);
                accountAddIntent.putExtra(AccountAddActivity.CLASS_NAME_EXTRA, classNames[item]);
                startActivityForResult(accountAddIntent, requestCodes[item]);
            }
        });
        return builder.create();
    }
}
