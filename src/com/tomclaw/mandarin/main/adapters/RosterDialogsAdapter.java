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
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.BuddyInfoTask;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:54 PM
 */
public class RosterDialogsAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_DIALOGS_ID = -2;

    /**
     * Columns
     */
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_ROSTER_BUDDY_ID;
    private static int COLUMN_ROSTER_BUDDY_NICK;
    private static int COLUMN_ROSTER_BUDDY_STATUS;
    private static int COLUMN_ROSTER_BUDDY_STATUS_TITLE;
    private static int COLUMN_ROSTER_BUDDY_STATUS_MESSAGE;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE;
    private static int COLUMN_ROSTER_BUDDY_UNREAD_COUNT;
    private static int COLUMN_ROSTER_BUDDY_AVATAR_HASH;
    private static int COLUMN_ROSTER_BUDDY_DRAFT;
    private static int COLUMN_ROSTER_BUDDY_LAST_TYPING;

    /**
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;
    private RosterAdapterCallback adapterCallback;

    public RosterDialogsAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        // Initialize loader for dialogs Id.
        loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Notifying listener.
        if (adapterCallback != null) {
            adapterCallback.onRosterUpdate();
        }
        return new CursorLoader(context,
                Settings.BUDDY_RESOLVER_URI, null, GlobalProvider.ROSTER_BUDDY_DIALOG + "='" + 1 + "'",
                null, "(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT + " > 0 THEN 2 ELSE 0 END) DESC, "
                + "(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_STATUS + "=" + StatusUtil.STATUS_OFFLINE
                + " THEN 0 ELSE 1 END" + ") DESC, "
                + GlobalProvider.ROSTER_BUDDY_NICK + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
        COLUMN_ROSTER_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
        COLUMN_ROSTER_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
        COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
        COLUMN_ROSTER_BUDDY_STATUS_TITLE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE);
        COLUMN_ROSTER_BUDDY_STATUS_MESSAGE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE);
        COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE);
        COLUMN_ROSTER_BUDDY_UNREAD_COUNT = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT);
        COLUMN_ROSTER_BUDDY_AVATAR_HASH = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH);
        COLUMN_ROSTER_BUDDY_DRAFT = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DRAFT);
        COLUMN_ROSTER_BUDDY_LAST_TYPING = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_LAST_TYPING);
        swapCursor(cursor);
        // Notifying listener.
        if (adapterCallback != null && cursor.getCount() == 0) {
            adapterCallback.onRosterEmpty();
        }
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
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        View view;
        try {
            if (cursor == null || !cursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                view = newView(context, cursor, parent);
            } else {
                view = convertView;
            }
            bindView(view, context, cursor);
        } catch (Throwable ex) {
            view = inflater.inflate(R.layout.buddy_item, parent, false);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.buddy_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        // Status image.
        String accountType = cursor.getString(COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE);
        int statusIndex = cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS);
        int statusImageResource = StatusUtil.getStatusDrawable(accountType, statusIndex);
        // Status text.
        String statusTitle = cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_TITLE);
        String statusMessage = cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_MESSAGE);
        if (statusIndex == StatusUtil.STATUS_OFFLINE
                || TextUtils.equals(statusTitle, statusMessage)) {
            // Buddy status is offline now or status message is only status title.
            // No status message could be displayed.
            statusMessage = "";
        }
        SpannableString statusString;
        long lastTyping = cursor.getLong(COLUMN_ROSTER_BUDDY_LAST_TYPING);
        // Checking for typing no more than 5 minutes.
        if (lastTyping > 0 && System.currentTimeMillis() - lastTyping < 5 * 60 * 1000) {
            statusString = new SpannableString(context.getString(R.string.typing));
        } else {
            statusString = new SpannableString(statusTitle + " " + statusMessage);
            statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, statusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Unread count.
        int unreadCount = cursor.getInt(COLUMN_ROSTER_BUDDY_UNREAD_COUNT);
        // Applying values.
        ((TextView) view.findViewById(R.id.buddy_nick)).setText(cursor.getString(COLUMN_ROSTER_BUDDY_NICK));
        ((ImageView) view.findViewById(R.id.buddy_status)).setImageResource(statusImageResource);
        ((TextView) view.findViewById(R.id.buddy_status_message)).setText(statusString);
        if (unreadCount > 0) {
            view.findViewById(R.id.counter_layout).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.counter_text)).setText(String.valueOf(unreadCount));
        } else {
            view.findViewById(R.id.counter_layout).setVisibility(View.GONE);
        }
        // Draft message.
        String buddyDraft = cursor.getString(COLUMN_ROSTER_BUDDY_DRAFT);
        view.findViewById(R.id.draft_indicator).setVisibility(
                TextUtils.isEmpty(buddyDraft) ? View.GONE : View.VISIBLE);
        // Avatar.
        final String avatarHash = cursor.getString(COLUMN_ROSTER_BUDDY_AVATAR_HASH);
        QuickContactBadge contactBadge = ((QuickContactBadge) view.findViewById(R.id.buddy_badge));
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.ic_default_avatar);
        // On-avatar click listener.
        final int buddyDbId = cursor.getInt(COLUMN_ROW_AUTO_ID);
        final BuddyInfoTask buddyInfoTask = new BuddyInfoTask(context, buddyDbId);
        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskExecutor.getInstance().execute(buddyInfoTask);
            }
        });
    }

    public int getBuddyDbId(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getInt(COLUMN_ROW_AUTO_ID);
    }

    public RosterAdapterCallback getAdapterCallback() {
        return adapterCallback;
    }

    public void setAdapterCallback(RosterAdapterCallback adapterCallback) {
        this.adapterCallback = adapterCallback;
    }

    public interface RosterAdapterCallback {

        public void onRosterUpdate();

        public void onRosterEmpty();
    }
}
