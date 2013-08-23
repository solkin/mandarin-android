package com.tomclaw.mandarin.main.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.main.HistorySelection;

import java.text.SimpleDateFormat;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/7/13
 * Time: 11:43 PM
 */
public class ChatHistoryAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {

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

    /**
     * Date and time format helpers
     */
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
    private static final SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm");

    /**
     * Adapter ID, equals to buddy db id of this chat
     */
    private final int ADAPTER_ID;

    private static int COLUMN_MESSAGE_TEXT;
    private static int COLUMN_MESSAGE_TIME;
    private static int COLUMN_MESSAGE_TYPE;
    private static int COLUMN_MESSAGE_STATE;
    private static int COLUMN_MESSAGE_READ_STATE;
    private static int COLUMN_MESSAGE_ACCOUNT_DB_ID;
    private static int COLUMN_MESSAGE_BUDDY_DB_ID;

    private Context context;
    private LayoutInflater mInflater;
    private Runnable onUpdate;
    // onUpdate must be called only once
    private boolean isUpdate = false;

    public ChatHistoryAdapter(Context context, LoaderManager loaderManager, int buddyBdId, Runnable onUpdate) {
        super(context, null, 0x00);
        this.context = context;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onUpdate = onUpdate;
        ADAPTER_ID = buddyBdId;
        // Initialize loader for adapter Id.
        loaderManager.initLoader(ADAPTER_ID, null, this);
        //isUpdate = false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(context, Settings.HISTORY_RESOLVER_URI, null,
                GlobalProvider.HISTORY_BUDDY_DB_ID + "='" + ADAPTER_ID + "'", null,
                GlobalProvider.ROW_AUTO_ID + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Detecting columns.
        COLUMN_MESSAGE_TEXT = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
        COLUMN_MESSAGE_TIME = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME);
        COLUMN_MESSAGE_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE);
        COLUMN_MESSAGE_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_STATE);
        COLUMN_MESSAGE_READ_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_READ_STATE);
        COLUMN_MESSAGE_ACCOUNT_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
        COLUMN_MESSAGE_BUDDY_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
        // Changing current cursor.
        swapCursor(cursor);

        if (!isUpdate){
            onUpdate.run();
            isUpdate = true;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        swapCursor(null);
    }

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        try {
            if (!mDataValid) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                view = newView(mContext, mCursor, parent);
            } else {
                view = convertView;
            }
            bindView(view, mContext, mCursor);
        } catch (Throwable ex) {
            if (convertView == null) {
                view = mInflater.inflate(R.layout.chat_item, parent, false);
                Log.d(Settings.LOG_TAG, "create new error view");
            } else {
                view = convertView;
                Log.d(Settings.LOG_TAG, "using existing view for error bubble");
            }
            // Update visibility.
            view.findViewById(R.id.date_layout).setVisibility(View.GONE);
            view.findViewById(R.id.outgoing_message).setVisibility(View.GONE);
            view.findViewById(R.id.incoming_message).setVisibility(View.GONE);
            view.findViewById(R.id.error_message).setVisibility(View.VISIBLE);
            Log.d(Settings.LOG_TAG, "exception in getView: " + ex.getMessage());
        }
        return view;
    }

    /**
     * Inflates view(s) from the specified XML file.
     *
     * @see android.widget.CursorAdapter#newView(android.content.Context,
     *      android.database.Cursor, ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.chat_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Message data.
        int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
        String messageText = cursor.getString(COLUMN_MESSAGE_TEXT);
        long messageTime = cursor.getLong(COLUMN_MESSAGE_TIME);
        int messageState = cursor.getInt(COLUMN_MESSAGE_STATE);
        int messageReadState = cursor.getInt(COLUMN_MESSAGE_READ_STATE);
        String messageTimeText = simpleTimeFormat.format(messageTime);
        String messageDateText = simpleDateFormat.format(messageTime);
        // Selected flag check box.
        view.findViewById(R.id.selected_check).setVisibility(
                HistorySelection.getInstance().getSelectionMode() ? View.VISIBLE : View.GONE);
        ((CheckBox) view.findViewById(R.id.selected_check)).setChecked(
                HistorySelection.getInstance().isSelectionExist(cursor.getPosition()));
        // Select message type.
        switch (MESSAGE_TYPES[messageType]) {
            case R.id.incoming_message: {
                // Update visibility.
                view.findViewById(R.id.incoming_message).setVisibility(View.VISIBLE);
                view.findViewById(R.id.outgoing_message).setVisibility(View.GONE);
                view.findViewById(R.id.error_message).setVisibility(View.GONE);
                // Updating data.
                ((TextView) view.findViewById(R.id.inc_text)).setText(messageText + "\n" + String.valueOf(messageReadState));
                ((TextView) view.findViewById(R.id.inc_time)).setText(messageTimeText);
                break;
            }
            case R.id.outgoing_message: {
                // Update visibility.
                view.findViewById(R.id.outgoing_message).setVisibility(View.VISIBLE);
                view.findViewById(R.id.incoming_message).setVisibility(View.GONE);
                view.findViewById(R.id.error_message).setVisibility(View.GONE);
                // Updating data.
                ((TextView) view.findViewById(R.id.out_text)).setText(messageText);
                ((TextView) view.findViewById(R.id.out_time)).setText(messageTimeText);
                ((ImageView) view.findViewById(R.id.message_delivery)).setImageResource(MESSAGE_STATES[messageState]);
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
                .equals(simpleDateFormat.format(cursor.getLong(COLUMN_MESSAGE_TIME))))) {
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

    public String getItemText(int position) {
        if (mCursor.moveToPosition(position)) {
            // Message data.
            int messageType = mCursor.getInt(COLUMN_MESSAGE_TYPE);
            String messageText = mCursor.getString(COLUMN_MESSAGE_TEXT);
            long messageTime = mCursor.getLong(COLUMN_MESSAGE_TIME);
            String messageTimeText = simpleTimeFormat.format(messageTime);
            String messageDateText = simpleDateFormat.format(messageTime);
            int accountDbId = mCursor.getInt(COLUMN_MESSAGE_ACCOUNT_DB_ID);
            int buddyDbId = mCursor.getInt(COLUMN_MESSAGE_BUDDY_DB_ID);
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
        return null;
    }
}