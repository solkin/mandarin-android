package com.tomclaw.mandarin.core;

import android.app.ProgressDialog;
import android.content.Context;
import com.tomclaw.mandarin.R;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 09.11.13
 * Time: 14:19
 */
public abstract class PleaseWaitTask extends Task {

    private final WeakReference<Context> weakContext;
    private WeakReference<ProgressDialog> weakProgressDialog;
    private int waitStringId = R.string.please_wait;

    public PleaseWaitTask(Context context) {
        this.weakContext = new WeakReference<Context>(context);
    }

    @Override
    public boolean isPreExecuteRequired() {
        return true;
    }

    @Override
    public void onPreExecuteMain() {
        Context context = weakContext.get();
        if(context != null) {
            ProgressDialog progressDialog = ProgressDialog.show(context, null, context.getString(waitStringId));
            weakProgressDialog = new WeakReference<ProgressDialog>(progressDialog);
        }
    }

    @Override
    public void onPostExecuteMain() {
        ProgressDialog progressDialog = weakProgressDialog.get();
        if(progressDialog != null) {
            progressDialog.hide();
        }
    }

    public WeakReference<Context> getWeakContext() {
        return weakContext;
    }
}
