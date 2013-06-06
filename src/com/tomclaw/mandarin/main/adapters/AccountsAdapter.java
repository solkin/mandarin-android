package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.main.StatusActitvity;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:00 PM
 */
public class AccountsAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private int COLUMN_USER_ID;
    private int COLUMN_USER_NICK;

    /** Adapter ID **/
    private final int ADAPTER_ID = 0x01;

    private LayoutInflater inflater;
    private Context context;

    public AccountsAdapter(Context context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        inflater = ((Activity) context).getLayoutInflater();
        // Initialize loader for adapter Id.
        loaderManager.initLoader(ADAPTER_ID, null, this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            Cursor cursor = getCursor();
            if (cursor == null || !cursor.moveToPosition(position)) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (convertView == null) {
                view = newView(context, cursor, parent);
            } else {
                view = convertView;
            }
            bindView(view, context, cursor);
        } catch (Throwable ex) {
            view = inflater.inflate(R.layout.account_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in roster general adapter: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.account_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Setup text values
        ((TextView) view.findViewById(R.id.user_id)).setText(cursor.getString(COLUMN_USER_ID));
        ((TextView) view.findViewById(R.id.user_nick)).setText(cursor.getString(COLUMN_USER_NICK));
        // Creating listeners for status click
        /*view.findViewById(R.id.user_status).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.startActivity(new Intent(context, StatusActitvity.class));
                    }
                });*/
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(context, Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
        COLUMN_USER_NICK = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        swapCursor(null);
    }
}
