package com.tomclaw.mandarin.im;

import android.database.Cursor;

import com.tomclaw.mandarin.core.GlobalProvider;

import java.io.Closeable;

/**
 * Created by ivsolkin on 26.11.16.
 */

public class MessageCursor implements Closeable {

    private Cursor cursor;
    private boolean isColumnsRead;

    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_BUDDY_ACCOUNT_DB_ID;
    private static int COLUMN_BUDDY_ID;
    private static int COLUMN_MESSAGE_PREV_ID;
    private static int COLUMN_MESSAGE_ID;
    private static int COLUMN_MESSAGE_COOKIE;
    private static int COLUMN_MESSAGE_TYPE;
    private static int COLUMN_MESSAGE_TIME;
    private static int COLUMN_MESSAGE_TEXT;
    private static int COLUMN_CONTENT_TYPE;
    private static int COLUMN_CONTENT_SIZE;
    private static int COLUMN_CONTENT_STATE;
    private static int COLUMN_CONTENT_PROGRESS;
    private static int COLUMN_CONTENT_URI;
    private static int COLUMN_CONTENT_NAME;
    private static int COLUMN_PREVIEW_HASH;
    private static int COLUMN_CONTENT_TAG;

    public MessageCursor() {
    }

    public MessageCursor(Cursor cursor) {
        switchCursor(cursor);
    }

    public void switchCursor(Cursor cursor) {
        this.cursor = cursor;
        readColumns();
    }

    public void invalidateColumns() {
        isColumnsRead = false;
        readColumns();
    }

    private void readColumns() {
        if (!isColumnsRead && cursor != null) {
            COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            COLUMN_BUDDY_ACCOUNT_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
            COLUMN_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ID);
            COLUMN_MESSAGE_PREV_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_PREV_ID);
            COLUMN_MESSAGE_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_ID);
            COLUMN_MESSAGE_COOKIE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_COOKIE);
            COLUMN_MESSAGE_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE);
            COLUMN_MESSAGE_TIME = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME);
            COLUMN_MESSAGE_TEXT = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
            COLUMN_CONTENT_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TYPE);
            COLUMN_CONTENT_SIZE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_SIZE);
            COLUMN_CONTENT_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_STATE);
            COLUMN_CONTENT_PROGRESS = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_PROGRESS);
            COLUMN_CONTENT_URI = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_URI);
            COLUMN_CONTENT_NAME = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_NAME);
            COLUMN_PREVIEW_HASH = cursor.getColumnIndex(GlobalProvider.HISTORY_PREVIEW_HASH);
            COLUMN_CONTENT_TAG = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TAG);
            isColumnsRead = true;
        }
    }

    public long getMessageDbId() {
        return cursor.getLong(COLUMN_ROW_AUTO_ID);
    }

    public int getBuddyAccountDbId() {
        return cursor.getInt(COLUMN_BUDDY_ACCOUNT_DB_ID);
    }

    public String getBuddyId() {
        return cursor.getString(COLUMN_BUDDY_ID);
    }

    public long getMessagePrevId() {
        return cursor.getLong(COLUMN_MESSAGE_PREV_ID);
    }

    public long getMessageId() {
        return cursor.getLong(COLUMN_MESSAGE_ID);
    }

    public String getCookie() {
        return cursor.getString(COLUMN_MESSAGE_COOKIE);
    }

    public int getMessageType() {
        return cursor.getInt(COLUMN_MESSAGE_TYPE);
    }

    public long getMessageTime() {
        return cursor.getLong(COLUMN_MESSAGE_TIME);
    }

    public String getMessageText() {
        return cursor.getString(COLUMN_MESSAGE_TEXT);
    }

    public int getContentType() {
        return cursor.getInt(COLUMN_CONTENT_TYPE);
    }

    public long getContentSize() {
        return cursor.getLong(COLUMN_CONTENT_SIZE);
    }

    public int getContentState() {
        return cursor.getInt(COLUMN_CONTENT_STATE);
    }

    public int getContentProgress() {
        return cursor.getInt(COLUMN_CONTENT_PROGRESS);
    }

    public String getContentUri() {
        return cursor.getString(COLUMN_CONTENT_URI);
    }

    public String getContentName() {
        return cursor.getString(COLUMN_CONTENT_NAME);
    }

    public String getPreviewHash() {
        return cursor.getString(COLUMN_PREVIEW_HASH);
    }

    public String getContentTag() {
        return cursor.getString(COLUMN_CONTENT_TAG);
    }

    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    public boolean isAfterLast() {
        return cursor.isAfterLast();
    }

    public boolean moveToNext() {
        return cursor.moveToNext();
    }

    public boolean moveToPosition(int position) {
        return cursor.moveToPosition(position);
    }

    public int getCount() {
        return cursor.getCount();
    }

    public Cursor getRawCursor() {
        return cursor;
    }

    @Override
    public void close() {
        if (cursor != null) {
            cursor.close();
        }
    }

    public boolean isClosed() {
        return cursor.isClosed();
    }
}
