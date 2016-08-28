package com.tomclaw.mandarin.main.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.TimeHelper;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/28/13
 * Time: 9:54 PM
 */
public class RosterDialogsAdapter extends CursorRecyclerAdapter<RosterDialogsAdapter.DialogViewHolder> implements
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
    private TimeHelper timeHelper;

    private BuddyCursor buddyCursor;

    private final SelectionHelper<Integer> selectionHelper = new SelectionHelper<>();

    private SelectionModeListener selectionModeListener;
    private ClickListener clickListener;

    public RosterDialogsAdapter(Activity context, LoaderManager loaderManager) {
        super(null);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.buddyCursor = new BuddyCursor();
        this.timeHelper = new TimeHelper(context);
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
        if (cursor != null && !cursor.isClosed()) {
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
    public DialogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.buddy_item, parent, false);
        return new DialogViewHolder(view);
    }

    @Override
    public void onBindViewHolderCursor(DialogViewHolder holder, Cursor cursor) {
        holder.bind(selectionHelper, buddyCursor, timeHelper);
        holder.bindClickListeners(clickListener, selectionModeListener, selectionHelper,
                buddyCursor.getBuddyDbId());
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

    public void setAdapterCallback(RosterAdapterCallback adapterCallback) {
        this.adapterCallback = adapterCallback;
    }

    public void setSelectionModeListener(SelectionModeListener selectionModeListener) {
        this.selectionModeListener = selectionModeListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface RosterAdapterCallback {

        void onRosterLoadingStarted();

        void onRosterEmpty();

        void onRosterUpdate();
    }

    public interface SelectionModeListener {
        void onItemStateChanged(int buddyDbId);

        void onNothingSelected();

        void onLongClicked(int buddyDbId, SelectionHelper<Integer> selectionHelper);
    }

    public interface ClickListener {
        void onItemClicked(int buddyDbId);
    }

    static class DialogViewHolder extends RecyclerView.ViewHolder {

        private TextView buddyNick;
        private ImageView buddyStatus;
        private TextView buddyStatusMessage;
        private TextView counterText;
        private View counterLayout;
        private View draftIndicator;
        private ContactBadge contactBadge;

        DialogViewHolder(View itemView) {
            super(itemView);

            buddyNick = ((TextView) itemView.findViewById(R.id.buddy_nick));
            buddyStatus = ((ImageView) itemView.findViewById(R.id.buddy_status));
            buddyStatusMessage = ((TextView) itemView.findViewById(R.id.buddy_status_message));
            counterText = ((TextView) itemView.findViewById(R.id.counter_text));
            counterLayout = itemView.findViewById(R.id.counter_layout);
            draftIndicator = itemView.findViewById(R.id.draft_indicator);
            contactBadge = ((ContactBadge) itemView.findViewById(R.id.buddy_badge));
        }

        void bind(SelectionHelper<Integer> selectionHelper, BuddyCursor buddyCursor, TimeHelper timeHelper) {
            Context context = itemView.getContext();
            // Selection indicator.
            int[] attrs = new int[]{R.attr.selectableItemBackground};
            TypedArray ta = context.obtainStyledAttributes(attrs);
            Drawable drawableFromTheme = ta.getDrawable(0);
            if (selectionHelper.isChecked(buddyCursor.getBuddyDbId())) {
                int backColor = R.color.orange_normal;
                itemView.setBackgroundColor(itemView.getResources().getColor(backColor));
            } else {
                itemView.setBackgroundDrawable(drawableFromTheme);
            }
            ta.recycle();
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
                statusString = new SpannableString(itemView.getContext().getString(R.string.typing));
            } else {
                long lastSeen = buddyCursor.getBuddyLastSeen();
                if (lastSeen > 0) {
                    String lastSeenText;
                    String lastSeenDate = timeHelper.getShortFormattedDate(lastSeen * 1000);
                    String lastSeenTime = timeHelper.getFormattedTime(lastSeen * 1000);

                    Calendar today = Calendar.getInstance();
                    today = TimeHelper.clearTimes(today);

                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    yesterday = TimeHelper.clearTimes(yesterday);

                    if (lastSeen * 1000 > today.getTimeInMillis()) {
                        lastSeenText = context.getString(R.string.last_seen_time, lastSeenTime);
                    } else if (lastSeen * 1000 > yesterday.getTimeInMillis()) {
                        lastSeenText = context.getString(R.string.last_seen_date, context.getString(R.string.yesterday), lastSeenTime);
                    } else {
                        lastSeenText = context.getString(R.string.last_seen_date, lastSeenDate, lastSeenTime);
                    }

                    statusString = new SpannableString(lastSeenText);
                } else {
                    statusString = new SpannableString(statusTitle + " " + statusMessage);
                    statusString.setSpan(new StyleSpan(Typeface.BOLD), 0, statusTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            // Unread count.
            int unreadCount = buddyCursor.getBuddyUnreadCount();
            // Applying values.
            buddyNick.setText(buddyCursor.getBuddyNick());
            buddyStatus.setImageResource(statusImageResource);
            buddyStatusMessage.setText(statusString);
            if (unreadCount > 0) {
                counterLayout.setVisibility(View.VISIBLE);
                counterText.setText(String.valueOf(unreadCount));
            } else {
                counterLayout.setVisibility(View.GONE);
            }
            // Draft message.
            String buddyDraft = buddyCursor.getBuddyDraft();
            draftIndicator.setVisibility(TextUtils.isEmpty(buddyDraft) ? View.GONE : View.VISIBLE);
            // Avatar.
            final String avatarHash = buddyCursor.getBuddyAvatarHash();
            BitmapCache.getInstance().getBitmapAsync(contactBadge, avatarHash, R.drawable.def_avatar_x48, false);
            // On-avatar click listener.
            final int buddyDbId = buddyCursor.getBuddyDbId();
            final BuddyInfoTask buddyInfoTask = new BuddyInfoTask(itemView.getContext(), buddyDbId);
            contactBadge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskExecutor.getInstance().execute(buddyInfoTask);
                }
            });
        }

        void bindClickListeners(final ClickListener clickListener,
                                final SelectionModeListener selectionModeListener,
                                final SelectionHelper<Integer> selectionHelper,
                                final int buddyDbId) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectionHelper.isSelectionMode()) {
                        selectionHelper.toggleChecked(buddyDbId);
                        selectionModeListener.onItemStateChanged(buddyDbId);
                        // Check for this was last selected item.
                        if (selectionHelper.isEmptySelection()) {
                            selectionModeListener.onNothingSelected();
                        }
                    } else {
                        clickListener.onItemClicked(buddyDbId);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    selectionModeListener.onLongClicked(buddyDbId, selectionHelper);
                    return true;
                }
            });
        }
    }
}
