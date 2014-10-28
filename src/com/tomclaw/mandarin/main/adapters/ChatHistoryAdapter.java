package com.tomclaw.mandarin.main.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.SmileyParser;
import com.tomclaw.mandarin.util.StringUtil;
import com.tomclaw.mandarin.util.TimeHelper;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/7/13
 * Time: 11:43 PM
 */
public class ChatHistoryAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int[][] ITEM_LAYOUTS = new int[][]{
            new int[]{R.layout.chat_item_error},
            new int[]{R.layout.chat_item_inc_text},
            new int[]{R.layout.chat_item_out_text, R.layout.chat_item_out_image, R.layout.chat_item_out_image, 0, R.layout.chat_item_out_file}
    };
    private static final int[] MESSAGE_TYPES = new int[]{
            R.id.error_message,
            R.id.incoming_message,
            R.id.outgoing_message};
    private static final int[] MESSAGE_STATES = new int[]{
            R.drawable.ic_dot,
            R.drawable.ic_error,
            R.drawable.ic_dot,
            R.drawable.ic_sent,
            R.drawable.ic_delivered
    };

    private TimeHelper timeHelper;

    /**
     * Adapter ID
     */
    private int buddyDbId = -1;

    private static int COLUMN_MESSAGE_TEXT;
    private static int COLUMN_MESSAGE_TIME;
    private static int COLUMN_MESSAGE_TYPE;
    private static int COLUMN_MESSAGE_STATE;
    private static int COLUMN_MESSAGE_ACCOUNT_DB_ID;
    private static int COLUMN_MESSAGE_BUDDY_DB_ID;
    private static int COLUMN_MESSAGE_READ;
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_CONTENT_TYPE;
    private static int COLUMN_CONTENT_SIZE;
    private static int COLUMN_CONTENT_STATE;
    private static int COLUMN_CONTENT_PROGRESS;
    private static int COLUMN_CONTENT_NAME;
    private static int COLUMN_PREVIEW_HASH;

    private Context context;
    private LayoutInflater inflater;
    private LoaderManager loaderManager;

    public ChatHistoryAdapter(Context context, LoaderManager loaderManager, int buddyBdId, TimeHelper timeHelper) {
        super(context, null, 0x00);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.loaderManager = loaderManager;
        this.timeHelper = timeHelper;
        setBuddyDbId(buddyBdId);
        setFilterQueryProvider(new ChatFilterQueryProvider());
        // Initialize smileys.
        SmileyParser.init(context);
    }

    private void setBuddyDbId(int buddyDbId) {
        if (buddyDbId >= 0) {
            // Destroy current loader.
            loaderManager.destroyLoader(buddyDbId);
        }
        this.buddyDbId = buddyDbId;
        // Initialize loader for adapter Id.
        loaderManager.initLoader(buddyDbId, null, this);
    }

    public int getBuddyDbId() {
        return buddyDbId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return getDefaultQueryBuilder().createCursorLoader(context, Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
        COLUMN_MESSAGE_TEXT = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
        COLUMN_MESSAGE_TIME = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME);
        COLUMN_MESSAGE_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE);
        COLUMN_MESSAGE_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_STATE);
        COLUMN_MESSAGE_ACCOUNT_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
        COLUMN_MESSAGE_BUDDY_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
        COLUMN_MESSAGE_READ = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_READ);
        COLUMN_CONTENT_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TYPE);
        COLUMN_CONTENT_SIZE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_SIZE);
        COLUMN_CONTENT_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_STATE);
        COLUMN_CONTENT_PROGRESS = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_PROGRESS);
        COLUMN_CONTENT_NAME = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_NAME);
        COLUMN_PREVIEW_HASH = cursor.getColumnIndex(GlobalProvider.HISTORY_PREVIEW_HASH);
        // Changing current cursor.
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

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
            view = inflater.inflate(R.layout.chat_item_error, parent, false);
            ex.printStackTrace();
        }
        return view;
    }

    /**
     * Inflates view(s) from the specified XML file.
     *
     * @see android.widget.CursorAdapter#newView(android.content.Context,
     * android.database.Cursor, ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
        int contentType = cursor.getInt(COLUMN_CONTENT_TYPE);
        return inflater.inflate(ITEM_LAYOUTS[messageType][contentType], parent, false);
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        int type;
        try {
            if (cursor == null || !cursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            type = cursor.getInt(COLUMN_MESSAGE_TYPE);
        } catch (Throwable ex) {
            type = 0;
        }
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Message data.
        int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
        CharSequence messageText = SmileyParser.getInstance().addSmileySpans(
                cursor.getString(COLUMN_MESSAGE_TEXT));
        long messageTime = cursor.getLong(COLUMN_MESSAGE_TIME);
        int messageState = cursor.getInt(COLUMN_MESSAGE_STATE);
        // Content message data
        int contentType = cursor.getInt(COLUMN_CONTENT_TYPE);
        long contentSize = cursor.getLong(COLUMN_CONTENT_SIZE);
        int contentState = cursor.getInt(COLUMN_CONTENT_STATE);
        int contentProgress = cursor.getInt(COLUMN_CONTENT_PROGRESS);
        String contentName = cursor.getString(COLUMN_CONTENT_NAME);
        String previewHash = cursor.getString(COLUMN_PREVIEW_HASH);
        String messageTimeText = timeHelper.getFormattedTime(messageTime);
        String messageDateText = timeHelper.getFormattedDate(messageTime);
        // Select message type.
        switch (MESSAGE_TYPES[messageType]) {
            case R.id.incoming_message: {
                // Updating data.
                ((TextView) view.findViewById(R.id.inc_text)).setText(messageText);
                ((TextView) view.findViewById(R.id.inc_time)).setText(messageTimeText);
                break;
            }
            case R.id.outgoing_message: {
                // Updating data.
                ((TextView) view.findViewById(R.id.out_time)).setText(messageTimeText);
                ((ImageView) view.findViewById(R.id.message_delivery)).setImageResource(MESSAGE_STATES[messageState]);
                // Updating content-specific data.
                switch (contentType) {
                    case GlobalProvider.HISTORY_CONTENT_TYPE_TEXT: {
                        TextView outText = (TextView) view.findViewById(R.id.out_text);
                        outText.setText(messageText);
                        break;
                    }
                    case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                    case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE: {
                        View outPreviewProgress = view.findViewById(R.id.out_preview_progress);
                        ImageView outPreviewImage = (ImageView) view.findViewById(R.id.out_preview_image);
                        View outError = view.findViewById(R.id.out_error);
                        View outProgressContainer = view.findViewById(R.id.out_progress_container);
                        ProgressBar outProgress = (ProgressBar) view.findViewById(R.id.out_progress);
                        TextView outSize = (TextView) view.findViewById(R.id.out_size);
                        BitmapCache.getInstance().getBitmapAsync(outPreviewImage, previewHash, android.R.color.transparent, true);
                        switch (contentState) {
                            case GlobalProvider.HISTORY_CONTENT_STATE_WAITING: {
                                outProgressContainer.setVisibility(View.GONE);
                                outError.setVisibility(View.GONE);
                                outPreviewProgress.setVisibility(View.VISIBLE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_RUNNING: {
                                outProgressContainer.setVisibility(View.VISIBLE);
                                outError.setVisibility(View.GONE);
                                outPreviewProgress.setVisibility(View.VISIBLE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_FAILED: {
                                outProgressContainer.setVisibility(View.GONE);
                                outError.setVisibility(View.VISIBLE);
                                outPreviewProgress.setVisibility(View.GONE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_STABLE: {
                                outProgressContainer.setVisibility(View.GONE);
                                outError.setVisibility(View.GONE);
                                outPreviewProgress.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                        outProgress.setProgress(contentProgress);
                        outSize.setText(StringUtil.formatBytes(context.getResources(), contentSize));
                        break;
                    }
                    case GlobalProvider.HISTORY_CONTENT_TYPE_FILE: {
                        TextView outName = (TextView) view.findViewById(R.id.out_name);
                        TextView outSize = (TextView) view.findViewById(R.id.out_size);
                        ProgressBar outProgress = (ProgressBar) view.findViewById(R.id.out_progress);
                        View outProgressContainer = view.findViewById(R.id.out_progress_container);

                        switch (contentState) {
                            case GlobalProvider.HISTORY_CONTENT_STATE_WAITING: {
                                outProgressContainer.setVisibility(View.GONE);
                                // outError.setVisibility(View.GONE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_RUNNING: {
                                outProgressContainer.setVisibility(View.VISIBLE);
                                // outError.setVisibility(View.GONE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_FAILED: {
                                outProgressContainer.setVisibility(View.GONE);
                                // outError.setVisibility(View.VISIBLE);
                                break;
                            }
                            case GlobalProvider.HISTORY_CONTENT_STATE_STABLE: {
                                outProgressContainer.setVisibility(View.GONE);
                                // outError.setVisibility(View.GONE);
                                break;
                            }
                        }

                        outName.setText(contentName);
                        outSize.setText(StringUtil.formatBytes(context.getResources(), contentSize));
                        outProgress.setProgress(contentProgress);
                        break;
                    }
                }
                break;
            }
            default: {
                // What's up?
                return;
            }
        }
        // Showing or hiding date.
        // Go to previous message and comparing dates.
        if (!(cursor.moveToPrevious() && messageDateText
                .equals(timeHelper.getFormattedDate(cursor.getLong(COLUMN_MESSAGE_TIME))))) {
            // Update visibility.
            view.findViewById(R.id.date_layout).setVisibility(View.VISIBLE);
            // Update date text view.
            ((TextView) view.findViewById(R.id.message_date))
                    .setText(messageDateText);
        } else {
            // Update visibility.
            view.findViewById(R.id.date_layout).setVisibility(View.GONE);
        }
    }

    public long getMessageDbId(int position) throws MessageNotFoundException {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new MessageNotFoundException();
        }
        return cursor.getLong(COLUMN_ROW_AUTO_ID);
    }

    public String getMessageText(int position) throws MessageNotFoundException {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            // Message data.
            int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
            String messageText = cursor.getString(COLUMN_MESSAGE_TEXT);
            long messageTime = cursor.getLong(COLUMN_MESSAGE_TIME);
            String messageTimeText = timeHelper.getFormattedTime(messageTime);
            String messageDateText = timeHelper.getFormattedDate(messageTime);
            int accountDbId = cursor.getInt(COLUMN_MESSAGE_ACCOUNT_DB_ID);
            int buddyDbId = cursor.getInt(COLUMN_MESSAGE_BUDDY_DB_ID);
            String buddyNick = "unknown";
            try {
                // Select message type.
                switch (MESSAGE_TYPES[messageType]) {
                    case R.id.incoming_message: {
                        buddyNick = QueryHelper.getBuddyNick(context.getContentResolver(), buddyDbId);
                        break;
                    }
                    case R.id.outgoing_message: {
                        buddyNick = QueryHelper.getAccountName(context.getContentResolver(), accountDbId);
                        break;
                    }
                }
            } catch (BuddyNotFoundException ignored) {
            } catch (AccountNotFoundException ignored) {
            }
            // Building message copy.
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append('[').append(buddyNick).append(']').append('\n');
            messageBuilder.append(messageDateText).append(" - ").append(messageTimeText).append('\n');
            messageBuilder.append(messageText);
            return messageBuilder.toString();
        }
        throw new MessageNotFoundException();
    }

    private QueryBuilder getDefaultQueryBuilder() {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        queryBuilder.ascending(GlobalProvider.ROW_AUTO_ID);
        return queryBuilder;
    }

    private class ChatFilterQueryProvider implements FilterQueryProvider {

        @Override
        public Cursor runQuery(CharSequence constraint) {
            String searchField = constraint.toString().toUpperCase();
            QueryBuilder queryBuilder = getDefaultQueryBuilder();
            queryBuilder.and().likeIgnoreCase(GlobalProvider.HISTORY_SEARCH_FIELD, searchField);
            return queryBuilder.query(context.getContentResolver(), Settings.HISTORY_RESOLVER_URI);
        }
    }
}