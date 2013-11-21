package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusUtil;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:00 PM
 */
public class AccountsAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Adapter ID
     */
    private final int ADAPTER_ID = 0x01;

    /**
     * Columns
     */
    private static int COLUMN_USER_ID;
    private static int COLUMN_USER_NICK;
    private static int COLUMN_USER_STATUS;
    private static int COLUMN_ACCOUNT_TYPE;
    private static int COLUMN_ACCOUNT_CONNECTING;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;

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
        ImageView userStatus = ((ImageView) view.findViewById(R.id.user_status));
        userStatus.setImageResource(
                StatusUtil.getStatusDrawable(
                        cursor.getString(COLUMN_ACCOUNT_TYPE),
                        cursor.getInt(COLUMN_USER_STATUS)));
        if (cursor.getInt(COLUMN_ACCOUNT_CONNECTING) == 1) {
            userStatus.setColorFilter(0xaaffffff);
        } else {
            userStatus.clearColorFilter();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(context, Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
        COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
        COLUMN_USER_NICK = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
        COLUMN_USER_STATUS = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS);
        COLUMN_ACCOUNT_CONNECTING = cursor.getColumnIndex(GlobalProvider.ACCOUNT_CONNECTING);
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        swapCursor(null);
    }
}
