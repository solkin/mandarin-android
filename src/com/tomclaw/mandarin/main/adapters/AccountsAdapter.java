package com.tomclaw.mandarin.main.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
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
import android.widget.QuickContactBadge;
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
    private OnAccountsStateListener onAccountsStateListener;

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
        final int statusIndex = cursor.getInt(COLUMN_USER_STATUS);
        final boolean isConnecting = cursor.getInt(COLUMN_ACCOUNT_CONNECTING) == 1;
        final String accountType = cursor.getString(COLUMN_ACCOUNT_TYPE);
        final boolean isConnected = (!isConnecting && statusIndex != StatusUtil.STATUS_OFFLINE);

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

        int statusMessageHint;
        if (isConnecting) {
            statusImage.setColorFilter(CONNECTING_STATUS_FILTER);
            statusMessage = "";
            statusMessageHint = (statusIndex == StatusUtil.STATUS_OFFLINE ?
                    R.string.disconnecting : R.string.connecting);
        } else {
            statusImage.clearColorFilter();
            if (statusIndex == StatusUtil.STATUS_OFFLINE) {
                statusMessageHint = R.string.click_to_connect_hint;
            } else {
                statusMessageHint = R.string.status_message_hint;
            }
        }

        statusMessageView.setText(statusMessage);
        statusMessageView.setHint(statusMessageHint);


        // Avatar.
        final String avatarHash = cursor.getString(COLUMN_ACCOUNT_AVATAR_HASH);
        QuickContactBadge contactBadge = ((QuickContactBadge) view.findViewById(R.id.user_badge));
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar, false);

        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAvatarClickListener != null) {

                    onAvatarClickListener.onAvatarClicked(accountDbId, isConnected);
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
        checkAccountsState();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Cursor cursor = swapCursor(null);
        // Maybe, previous non-closed cursor present?
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    /**
     * Checking for accounts state.
     * If any one connecting or disconnecting - first priority event.
     * If eny account is online - second priority event.
     * Offline event only if all accounts are offline.
     * Also, NoAccounts if accounts table is empty.
     */
    private void checkAccountsState() {
        OnAccountsStateListener.AccountsState state = OnAccountsStateListener.AccountsState.Offline;
        Cursor cursor = getCursor();
        if (cursor == null || cursor.getCount() == 0) {
            onAccountsStateListener.onAccountsStateChanged(OnAccountsStateListener.AccountsState.NoAccounts);
            return;
        }
        for (int c = 0; c < cursor.getCount(); c++) {
            cursor.moveToPosition(c);
            int status = cursor.getInt(COLUMN_USER_STATUS);
            boolean connecting = (cursor.getInt(COLUMN_ACCOUNT_CONNECTING) == 1);
            if (connecting) {
                // We are changing status to offline.
                if (status == StatusUtil.STATUS_OFFLINE) {
                    onAccountsStateListener.onAccountsStateChanged(OnAccountsStateListener.AccountsState.Disconnecting);
                } else {
                    onAccountsStateListener.onAccountsStateChanged(OnAccountsStateListener.AccountsState.Connecting);
                }
                return;
            }
            if (status != StatusUtil.STATUS_OFFLINE) {
                state = OnAccountsStateListener.AccountsState.Online;
            }
        }
        onAccountsStateListener.onAccountsStateChanged(state);
    }

    public void setOnAvatarClickListener(OnAvatarClickListener onAvatarClickListener) {
        this.onAvatarClickListener = onAvatarClickListener;
    }

    public void setOnAccountsStateListener(OnAccountsStateListener onAccountsStateListener) {
        this.onAccountsStateListener = onAccountsStateListener;
    }

    public interface OnAvatarClickListener {

        public void onAvatarClicked(int accountDbId, boolean isConnected);
    }

    public interface OnAccountsStateListener {

        enum AccountsState {
            NoAccounts,
            Offline,
            Disconnecting,
            Connecting,
            Online
        }

        public void onAccountsStateChanged(AccountsState state);
    }
}
