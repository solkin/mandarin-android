package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
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
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.views.ContactBadge;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by solkin on 04.08.14.
 */
@SuppressWarnings("WeakerAccess")
public abstract class RosterStickyAdapter extends CursorAdapter
        implements LoaderManager.LoaderCallbacks<Cursor>, StickyListHeadersAdapter {

    /**
     * Adapter ID
     */
    private static final int ADAPTER_STICKY_ID = -1;

    /**
     * Filter
     */
    public static final int FILTER_ALL_BUDDIES = 0x00;
    public static final int FILTER_ONLINE_ONLY = 0x01;

    /**
     * Variables
     */
    private Context context;
    protected LayoutInflater inflater;
    private int filter;
    private boolean isShowTemp;
    private LoaderManager loaderManager;

    private BuddyCursor buddyCursor;

    public RosterStickyAdapter(Activity context, LoaderManager loaderManager, int filter) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = context.getLayoutInflater();
        this.loaderManager = loaderManager;
        this.filter = filter;
        this.isShowTemp = PreferenceHelper.isShowTemp(context);
        this.buddyCursor = new BuddyCursor();
        initLoader();
        setFilterQueryProvider(new RosterFilterQueryProvider());
    }

    public void initLoader() {
        // Initialize loader for dialogs Id.
        loaderManager.restartLoader(ADAPTER_STICKY_ID, null, this);
    }

    public Context getContext() {
        return context;
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
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.buddy_item, parent, false);
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
        SpannableString statusString = new SpannableString(statusTitle + " " + statusMessage);
        statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, statusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        ContactBadge contactBadge = view.findViewById(R.id.buddy_badge);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return getDefaultQueryBuilder().createCursorLoader(context, Settings.BUDDY_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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

    public BuddyCursor getBuddyCursor() {
        return buddyCursor;
    }

    public int getBuddyDbId(int position) {
        BuddyCursor cursor = getBuddyCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return cursor.getBuddyDbId();
    }

    public void setRosterFilter(int filter) {
        this.filter = filter;
    }

    public int getRosterFilter() {
        return filter;
    }

    private QueryBuilder getDefaultQueryBuilder() {
        QueryBuilder queryBuilder = new QueryBuilder();
        switch (filter) {
            case FILTER_ONLINE_ONLY: {
                queryBuilder.columnNotEquals(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
                break;
            }
            case FILTER_ALL_BUDDIES:
            default:
        }
        if (!isShowTemp) {
            queryBuilder.and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        }
        queryBuilder.and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_OPERATION,
                GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE);
        postQueryBuilder(queryBuilder);
        return queryBuilder;
    }

    protected abstract void postQueryBuilder(QueryBuilder queryBuilder);

    private class RosterFilterQueryProvider implements FilterQueryProvider {

        @Override
        public Cursor runQuery(CharSequence constraint) {
            String searchField = constraint.toString().toUpperCase();
            QueryBuilder queryBuilder = getDefaultQueryBuilder();
            queryBuilder.and().startComplexExpression().like(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, searchField)
                    .or().like(GlobalProvider.ROSTER_BUDDY_ID, constraint).finishComplexExpression();
            return queryBuilder.query(context.getContentResolver(), Settings.BUDDY_RESOLVER_URI);
        }
    }
}
