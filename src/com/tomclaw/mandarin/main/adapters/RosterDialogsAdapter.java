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
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.views.ContactBadge;
import com.tomclaw.mandarin.util.Logger;

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
     * Variables
     */
    private Context context;
    private LayoutInflater inflater;
    private RosterAdapterCallback adapterCallback;

    private BuddyCursor buddyCursor;

    public RosterDialogsAdapter(Activity context, LoaderManager loaderManager) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.buddyCursor = new BuddyCursor();
        // Initialize loader for dialogs Id.
        loaderManager.initLoader(ADAPTER_DIALOGS_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Notifying listener.
        if (adapterCallback != null) {
            adapterCallback.onRosterLoadingStarted();
        }
        return new CursorLoader(context,
                Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_DIALOG + "=" + 1 + " AND "
                        + GlobalProvider.ROSTER_BUDDY_OPERATION + "!=" + GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE,
                null, "(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT + " > 0 THEN 1 ELSE 0 END) DESC, "
                + GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME + " DESC, "
                + GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        swapCursor(cursor);
        // Notifying listener.
        if (adapterCallback != null) {
            if (cursor.getCount() == 0) {
                adapterCallback.onRosterEmpty();
            } else {
                adapterCallback.onRosterUpdate();
            }
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

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        buddyCursor.switchCursor(newCursor);
        return super.swapCursor(newCursor);
    }

    public boolean moveToFirst() {
        return buddyCursor.moveToFirst();
    }

    public BuddyCursor getBuddyCursor() {
        return buddyCursor;
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
            Logger.log("exception in getView: " + ex.getMessage());
        }
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return inflater.inflate(R.layout.buddy_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Status image.
        String accountType = buddyCursor.getBuddyAccountType();
        int statusIndex = buddyCursor.getBuddyStatus();
        int statusImageResource = StatusUtil.getStatusDrawable(accountType, statusIndex);
        // Status text.
        String statusTitle = buddyCursor.getBuddyStatusTitle();
        String statusMessage = buddyCursor.getBuddyStatusMessage();
        if (statusIndex == StatusUtil.STATUS_OFFLINE
                || TextUtils.equals(statusTitle, statusMessage)) {
            // Buddy status is offline now or status message is only status title.
            // No status message could be displayed.
            statusMessage = "";
        }
        SpannableString statusString;
        long lastTyping = buddyCursor.getBuddyLastTyping();
        // Checking for typing no more than 5 minutes.
        if (lastTyping > 0 && System.currentTimeMillis() - lastTyping < Settings.TYPING_DELAY) {
            statusString = new SpannableString(context.getString(R.string.typing));
        } else {
            statusString = new SpannableString(statusTitle + " " + statusMessage);
            statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, statusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Unread count.
        int unreadCount = buddyCursor.getBuddyUnreadCount();
        // Applying values.
        ((TextView) view.findViewById(R.id.buddy_nick)).setText(buddyCursor.getBuddyNick());
        ((ImageView) view.findViewById(R.id.buddy_status)).setImageResource(statusImageResource);
        ((TextView) view.findViewById(R.id.buddy_status_message)).setText(statusString);
        if (unreadCount > 0) {
            view.findViewById(R.id.counter_layout).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.counter_text)).setText(String.valueOf(unreadCount));
        } else {
            view.findViewById(R.id.counter_layout).setVisibility(View.GONE);
        }
        // Draft message.
        String buddyDraft = buddyCursor.getBuddyDraft();
        view.findViewById(R.id.draft_indicator).setVisibility(
                TextUtils.isEmpty(buddyDraft) ? View.GONE : View.VISIBLE);
        // Avatar.
        final String avatarHash = buddyCursor.getBuddyAvatarHash();
        ContactBadge contactBadge = ((ContactBadge) view.findViewById(R.id.buddy_badge));
        BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.def_avatar_x48, false);
        // On-avatar click listener.
        final int buddyDbId = buddyCursor.getBuddyDbId();
        final BuddyInfoTask buddyInfoTask = new BuddyInfoTask(context, buddyDbId);
        contactBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskExecutor.getInstance().execute(buddyInfoTask);
            }
        });
    }

    public int getBuddyDbId(int position) {
        BuddyCursor cursor = getBuddyCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getBuddyDbId();
    }

    public RosterAdapterCallback getAdapterCallback() {
        return adapterCallback;
    }

    public void setAdapterCallback(RosterAdapterCallback adapterCallback) {
        this.adapterCallback = adapterCallback;
    }

    public interface RosterAdapterCallback {

        public void onRosterLoadingStarted();

        public void onRosterEmpty();

        public void onRosterUpdate();
    }
}
