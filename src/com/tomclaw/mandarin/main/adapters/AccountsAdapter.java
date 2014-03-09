package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
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

    private static final int CONNECTING_STATUS_COLOR_FILTER = 0xaaffffff;

    /**
     * Adapter ID
     */
    private final int ADAPTER_ID = 0x01;

    /**
     * Columns
     */
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_USER_ID;
    private static int COLUMN_USER_NICK;
    private static int COLUMN_USER_STATUS;
    private static int COLUMN_USER_STATUS_TITLE;
    private static int COLUMN_USER_STATUS_MESSAGE;
    private static int COLUMN_ACCOUNT_TYPE;
    private static int COLUMN_ACCOUNT_CONNECTING;
    private static int COLUMN_ACCOUNT_AVATAR_HASH;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;

    public AccountsAdapter(Context context, LoaderManager loaderManager) {
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
            view = inflater.inflate(R.layout.account_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in accounts adapter: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.account_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Setup text values.
        String userId = cursor.getString(COLUMN_USER_ID);
        String userNick = cursor.getString(COLUMN_USER_NICK);
        if(TextUtils.isEmpty(userNick)) {
            userNick = userId;
        }
        ((TextView) view.findViewById(R.id.user_nick)).setText(userNick);
        // Statuses.
        int statusIndex = cursor.getInt(COLUMN_USER_STATUS);
        int isConnecting = cursor.getInt(COLUMN_ACCOUNT_CONNECTING);
        String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
        ImageView userStatus = ((ImageView) view.findViewById(R.id.user_status));
        userStatus.setImageResource(
                StatusUtil.getStatusDrawable(accountType, statusIndex));
        SpannableString statusString;
        if (isConnecting == 1) {
            userStatus.setColorFilter(CONNECTING_STATUS_COLOR_FILTER);
            statusString = new SpannableString(statusIndex == StatusUtil.STATUS_OFFLINE ?
                    context.getString(R.string.disconnecting) : context.getString(R.string.connecting));
        } else {
            userStatus.clearColorFilter();
            // Stable status string.
            String statusTitle = cursor.getString(COLUMN_USER_STATUS_TITLE);
            String statusMessage = cursor.getString(COLUMN_USER_STATUS_MESSAGE);
            if (statusIndex == StatusUtil.STATUS_OFFLINE
                    || TextUtils.equals(statusTitle, statusMessage)) {
                // Buddy status is offline now or status message is only status title.
                // No status message could be displayed.
                statusTitle = StatusUtil.getStatusTitle(accountType, statusIndex);
                statusMessage = "";
            }
            statusString = new SpannableString(statusTitle + " " + statusMessage);
            statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, statusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ((TextView) view.findViewById(R.id.user_status_message)).setText(statusString);
        // Avatar.
        final String avatarHash = cursor.getString(COLUMN_ACCOUNT_AVATAR_HASH);
        ImageView contactBadge = ((ImageView) view.findViewById(R.id.user_badge));
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(context, Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
        COLUMN_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
        COLUMN_USER_ID = cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID);
        COLUMN_USER_NICK = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
        COLUMN_USER_STATUS = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS);
        COLUMN_USER_STATUS_TITLE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS_TITLE);
        COLUMN_USER_STATUS_MESSAGE = cursor.getColumnIndex(GlobalProvider.ACCOUNT_STATUS_MESSAGE);
        COLUMN_ACCOUNT_CONNECTING = cursor.getColumnIndex(GlobalProvider.ACCOUNT_CONNECTING);
        COLUMN_ACCOUNT_AVATAR_HASH = cursor.getColumnIndex(GlobalProvider.ACCOUNT_AVATAR_HASH);
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
}
