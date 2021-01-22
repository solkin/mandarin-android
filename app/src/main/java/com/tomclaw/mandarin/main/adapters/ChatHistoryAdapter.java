package com.tomclaw.mandarin.main.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.MessageCursor;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.views.history.BaseHistoryView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingFileView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingImageView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingTextView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingVideoView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingFileView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingImageView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingTextView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingVideoView;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.SmileyParser;
import com.tomclaw.helpers.TimeHelper;

import java.lang.reflect.Constructor;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/7/13
 * Time: 11:43 PM
 */
public class ChatHistoryAdapter extends CursorRecyclerAdapter<BaseHistoryView> implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 10;

    private static final int[] ITEM_LAYOUTS = new int[]{
            0,
            R.layout.chat_item_inc_text,
            R.layout.chat_item_inc_image,
            R.layout.chat_item_inc_video,
            R.layout.chat_item_inc_file,
            R.layout.chat_item_out_text,
            R.layout.chat_item_out_image,
            R.layout.chat_item_out_video,
            R.layout.chat_item_out_file
    };

    private static final Class[] ITEM_HOLDERS = new Class[]{
            null,
            IncomingTextView.class,
            IncomingImageView.class,
            IncomingVideoView.class,
            IncomingFileView.class,
            OutgoingTextView.class,
            OutgoingImageView.class,
            OutgoingVideoView.class,
            OutgoingFileView.class
    };

    private Context context;
    private LayoutInflater inflater;
    private LoaderManager loaderManager;
    private TimeHelper timeHelper;
    private Buddy buddy = null;
    private MessageCursor messageCursor;
    private ContentMessageClickListener contentMessageClickListener;
    private SelectionModeListener selectionModeListener;
    private HistoryIntegrityListener historyIntegrityListener;

    private final SelectionHelper<Long> selectionHelper = new SelectionHelper<>();

    public ChatHistoryAdapter(Context context, LoaderManager loaderManager,
                              Buddy buddy, TimeHelper timeHelper) {
        super(null);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.messageCursor = new MessageCursor();
        this.loaderManager = loaderManager;
        this.timeHelper = timeHelper;
        setBuddy(buddy);
        // Initialize smileys.
        SmileyParser.init(context);
        setHasStableIds(true);
    }

    private void setBuddy(Buddy buddy) {
        if (!TextUtils.isEmpty(buddy.getBuddyId())) {
            loaderManager.destroyLoader(LOADER_ID);
        }
        this.buddy = buddy;
        loaderManager.initLoader(LOADER_ID, null, this);
    }

    public Buddy getBuddy() {
        return buddy;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return getDefaultQueryBuilder().createCursorLoader(context, Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        close();
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        messageCursor.switchCursor(newCursor);
        return super.swapCursor(newCursor);
    }

    public void close() {
        Cursor cursor = swapCursor(null);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private MessageCursor getMessageCursor() {
        return messageCursor;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public BaseHistoryView onCreateViewHolder(@NonNull ViewGroup viewGroup, int messageType) {
        try {
            // Inflate view by type.
            View view = inflater.inflate(ITEM_LAYOUTS[messageType], viewGroup, false);
            Class clazz = ITEM_HOLDERS[messageType];
            // Instantiate holder for this view.
            Constructor<BaseHistoryView> constructor = clazz.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageCursor cursor = getMessageCursor();
        int type;
        try {
            if (cursor == null || !cursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            int messageType = cursor.getMessageType();
            int contentType = cursor.getContentType();
            type = getItemType(messageType, contentType);
        } catch (Throwable ex) {
            type = 0;
        }
        return type;
    }

    private int getItemType(int messageType, int contentType) {
        int type;
        switch (messageType) {
            case GlobalProvider.HISTORY_MESSAGE_TYPE_ERROR:
                type = 0;
                break;
            case GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING:
                switch (contentType) {
                    case GlobalProvider.HISTORY_CONTENT_TYPE_TEXT:
                        type = 1;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE:
                        type = 2;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                        type = 3;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_FILE:
                        type = 4;
                        break;
                    default:
                        return 0;
                }
                break;
            case GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING:
                switch (contentType) {
                    case GlobalProvider.HISTORY_CONTENT_TYPE_TEXT:
                        type = 5;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE:
                        type = 6;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                        type = 7;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_FILE:
                        type = 8;
                        break;
                    default:
                        return 0;
                }
                break;
            default:
                return 0;
        }
        return type;
    }

    private QueryBuilder getDefaultQueryBuilder() {
        return new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, buddy.getAccountDbId()).and()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ID, buddy.getBuddyId())
                .descending(GlobalProvider.HISTORY_MESSAGE_ID).andOrder()
                .descending(GlobalProvider.HISTORY_MESSAGE_TIME);
    }

    public void setContentMessageClickListener(
            ContentMessageClickListener contentMessageClickListener) {
        this.contentMessageClickListener = contentMessageClickListener;
    }

    public void setSelectionModeListener(SelectionModeListener selectionModeListener) {
        this.selectionModeListener = selectionModeListener;
    }

    public void setHistoryIntegrityListener(HistoryIntegrityListener historyIntegrityListener) {
        this.historyIntegrityListener = historyIntegrityListener;
    }

    @Override
    public void onBindViewHolderCursor(BaseHistoryView holder, Cursor cursor) {
        MessageCursor messageCursor = getMessageCursor();
        long messageDbId = messageCursor.getMessageDbId();
        long messageId = messageCursor.getMessageId();
        long messagePrevId = messageCursor.getMessagePrevId();
        int messageType = messageCursor.getMessageType();
        CharSequence messageText = SmileyParser.getInstance()
                .addSmileySpans(messageCursor.getMessageText());
        long messageTime = messageCursor.getMessageTime();
        final String messageCookie = messageCursor.getCookie();
        // Content message data
        int contentType = messageCursor.getContentType();
        long contentSize = messageCursor.getContentSize();
        final int contentState = messageCursor.getContentState();
        int contentProgress = messageCursor.getContentProgress();
        final String contentName = messageCursor.getContentName();
        final String contentUri = messageCursor.getContentUri();
        String previewHash = messageCursor.getPreviewHash();
        final String contentTag = messageCursor.getContentTag();
        String messageTimeText = timeHelper.getFormattedTime(messageTime);
        String messageDateText = timeHelper.getFormattedDate(messageTime);
        // Showing or hiding date.
        // Go to previous message and comparing dates.
        boolean isFirst = messageCursor.isFirst();
        boolean isMoved = messageCursor.moveToNext();
        boolean dateVisible = true;
        if (isMoved) {
            long movedMessageTime = messageCursor.getMessageTime();
            String movedMessageDateText = timeHelper.getFormattedDate(movedMessageTime);
            dateVisible = !messageDateText.equals(movedMessageDateText);
        }
        long upperMessageId = 0;
        if (isMoved) {
            upperMessageId = messageCursor.getMessageId();
        }
        if (messagePrevId == GlobalProvider.HISTORY_MESSAGE_ID_INVALID ||
                (isMoved && upperMessageId != messagePrevId &&
                        messagePrevId != GlobalProvider.HISTORY_MESSAGE_ID_REQUESTED)) {
            Logger.log("Hole between " + upperMessageId + " and " + messageId);
            if (historyIntegrityListener != null) {
                historyIntegrityListener.onHole(buddy, upperMessageId, messageId);
            }
        }
        if (isFirst) {
            if (historyIntegrityListener != null) {
                historyIntegrityListener.onHistoryUpdated(buddy);
            }
        }
        // Creating chat history item to bind the view.
        ChatHistoryItem historyItem = new ChatHistoryItem(messageId, messagePrevId, messageDbId,
                messageType, messageText, messageTime, messageCookie, contentType, contentSize,
                contentState, contentProgress, contentName, contentUri, previewHash, contentTag,
                messageTimeText, messageDateText, dateVisible);
        holder.setSelectionHelper(selectionHelper);
        holder.setContentClickListener(contentMessageClickListener);
        holder.setSelectionModeListener(selectionModeListener);
        holder.bind(historyItem);
    }

    public interface ContentMessageClickListener {

        void onClicked(ChatHistoryItem historyItem);
    }

    public interface SelectionModeListener {
        void onItemStateChanged(ChatHistoryItem historyItem);

        void onNothingSelected();

        void onLongClicked(ChatHistoryItem historyItem, SelectionHelper<Long> selectionHelper);
    }

    public interface HistoryIntegrityListener {

        void onHole(Buddy buddy, long fromMessageId, long tillMessageId);

        void onHistoryUpdated(Buddy buddy);
    }
}