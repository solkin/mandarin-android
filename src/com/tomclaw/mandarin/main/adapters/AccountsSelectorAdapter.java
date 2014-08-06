package com.tomclaw.mandarin.main.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created by Solkin on 13.06.2014.
 */
public class AccountsSelectorAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Adapter ID
     */
    private final int ADAPTER_ID = 0x03;

    /**
     * Columns
     */
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_USER_ID;
    private static int COLUMN_USER_NICK;
    private static int COLUMN_USER_STATUS;
    private static int COLUMN_ACCOUNT_TYPE;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;

    public AccountsSelectorAdapter(Context context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        inflater = LayoutInflater.from(context);
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
            view = inflater.inflate(R.layout.account_selector_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in accounts adapter: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.account_selector_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Setup text values.
        final int accountDbId = cursor.getInt(COLUMN_ROW_AUTO_ID);
        final String userId = cursor.getString(COLUMN_USER_ID);
        String userNick = cursor.getString(COLUMN_USER_NICK);

        ImageView statusImage = ((ImageView) view.findViewById(R.id.user_status));
        TextView userIdView = ((TextView) view.findViewById(R.id.user_id));
        if (TextUtils.isEmpty(userNick)) {
            userIdView.setVisibility(View.GONE);
            userNick = userId;
        } else {
            userIdView.setVisibility(View.VISIBLE);
        }
        ((TextView) view.findViewById(R.id.user_nick)).setText(userNick);
        userIdView.setText(userId);
        // Statuses.
        final int statusIndex = cursor.getInt(COLUMN_USER_STATUS);
        final String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);

        statusImage.setImageResource(StatusUtil.getStatusDrawable(accountType, statusIndex));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnNotEquals(GlobalProvider.ACCOUNT_STATUS, StatusUtil.STATUS_OFFLINE)
                .and().columnEquals(GlobalProvider.ACCOUNT_CONNECTING, 0);
        return queryBuilder.createCursorLoader(context, Settings.ACCOUNT_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
        COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
        COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
        COLUMN_USER_NICK = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
        COLUMN_USER_STATUS = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS);
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Cursor cursor = swapCursor(null);
        // Maybe, previous non-closed cursor present?
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public int getAccountDbId(int position) throws AccountNotFoundException {
        Cursor cursor = getCursor();
        if (cursor != null && !cursor.isClosed() && cursor.moveToPosition(position)) {
            return cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
        }
        throw new AccountNotFoundException();
    }
}
