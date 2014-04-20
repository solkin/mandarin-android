package com.tomclaw.mandarin.main.adapters;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.StatusNotFoundException;
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
    private int CONNECTING_STATUS_FILTER;

    /**
     * Listeners
     */
    private OnAvatarClickListener onAvatarClickListener;

    public AccountsAdapter(Context context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        inflater = LayoutInflater.from(context);
        // Initialize loader for adapter Id.
        loaderManager.initLoader(ADAPTER_ID, null, this);
        CONNECTING_STATUS_FILTER = context.getResources().getColor(R.color.connecting_status_filter);
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
        final int accountDbId = cursor.getInt(COLUMN_ROW_AUTO_ID);
        final String userId = cursor.getString(COLUMN_USER_ID);
        String userNick = cursor.getString(COLUMN_USER_NICK);
        if (TextUtils.isEmpty(userNick)) {
            userNick = userId;
        }
        ((TextView) view.findViewById(R.id.user_nick)).setText(userNick);
        TextView statusTitleView = ((TextView) view.findViewById(R.id.status_title));
        TextView statusMessageView = ((TextView) view.findViewById(R.id.status_message));
        ImageView statusImage = ((ImageView) view.findViewById(R.id.status_icon));
        // Statuses.
        int statusIndex = cursor.getInt(COLUMN_USER_STATUS);
        int isConnecting = cursor.getInt(COLUMN_ACCOUNT_CONNECTING);
        final String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);

        // Stable status string.
        String statusTitle = cursor.getString(COLUMN_USER_STATUS_TITLE);
        String statusMessage = cursor.getString(COLUMN_USER_STATUS_MESSAGE);
        if (statusIndex == StatusUtil.STATUS_OFFLINE
                || TextUtils.equals(statusTitle, statusMessage)) {
            // User status is offline now or status message is only status title.
            // No status message could be displayed.
            statusTitle = StatusUtil.getStatusTitle(accountType, statusIndex);
            statusMessage = "";
        }

        statusImage.setImageResource(StatusUtil.getStatusDrawable(accountType, statusIndex));
        statusTitleView.setText(statusTitle);

        if(isConnecting == 1) {
            statusImage.setColorFilter(CONNECTING_STATUS_FILTER);
            statusMessageView.setText("");
            statusMessageView.setHint(statusIndex == StatusUtil.STATUS_OFFLINE ?
                    R.string.disconnecting : R.string.connecting);
        } else {
            statusImage.clearColorFilter();
            statusMessageView.setText(statusMessage);
            statusMessageView.setHint(R.string.status_message_hint);
        }


        // Avatar.
        final String avatarHash = cursor.getString(COLUMN_ACCOUNT_AVATAR_HASH);
        QuickContactBadge contactBadge = ((QuickContactBadge) view.findViewById(R.id.user_badge));
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar);

        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onAvatarClickListener != null) {
                    onAvatarClickListener.onAvatarClicked(accountDbId);
                }
            }
        });
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
    public void setOnAvatarClickListener(OnAvatarClickListener onAvatarClickListener) {
        this.onAvatarClickListener = onAvatarClickListener;
    }

    public interface OnAvatarClickListener {

        public void onAvatarClicked(int accountDbId);
    }

}
