package com.tomclaw.mandarin.im;

import android.database.Cursor;

import com.tomclaw.mandarin.core.GlobalProvider;

import java.io.Closeable;

/**
 * Created by solkin on 23/03/14.
 */
public class BuddyCursor implements Closeable {

    private Cursor cursor;
    private boolean isColumnsRead;

    /**
     * Columns
     */
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_DB_ID;
    private static int COLUMN_ROSTER_BUDDY_ID;
    private static int COLUMN_ROSTER_BUDDY_NICK;
    private static int COLUMN_ROSTER_BUDDY_GROUP;
    private static int COLUMN_ROSTER_BUDDY_GROUP_ID;
    private static int COLUMN_ROSTER_BUDDY_DIALOG;
    private static int COLUMN_ROSTER_BUDDY_STATUS;
    private static int COLUMN_ROSTER_BUDDY_STATUS_TITLE;
    private static int COLUMN_ROSTER_BUDDY_STATUS_MESSAGE;
    private static int COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE;
    private static int COLUMN_ROSTER_BUDDY_ALPHABET_INDEX;
    private static int COLUMN_ROSTER_BUDDY_UNREAD_COUNT;
    private static int COLUMN_ROSTER_BUDDY_AVATAR_HASH;
    private static int COLUMN_ROSTER_BUDDY_DRAFT;
    private static int COLUMN_ROSTER_BUDDY_LAST_SEEN;
    private static int COLUMN_ROSTER_BUDDY_LAST_TYPING;
    private static int COLUMN_ROSTER_BUDDY_OPERATION;

    public BuddyCursor() {
    }

    public BuddyCursor(Cursor cursor) {
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
        if (!isColumnsRead) {
            COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            COLUMN_ROSTER_BUDDY_ACCOUNT_DB_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID);
            COLUMN_ROSTER_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID);
            COLUMN_ROSTER_BUDDY_NICK = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            COLUMN_ROSTER_BUDDY_GROUP = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_GROUP);
            COLUMN_ROSTER_BUDDY_GROUP_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_GROUP_ID);
            COLUMN_ROSTER_BUDDY_DIALOG = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DIALOG);
            COLUMN_ROSTER_BUDDY_STATUS = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS);
            COLUMN_ROSTER_BUDDY_STATUS_TITLE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE);
            COLUMN_ROSTER_BUDDY_STATUS_MESSAGE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE);
            COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE);
            COLUMN_ROSTER_BUDDY_ALPHABET_INDEX = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX);
            COLUMN_ROSTER_BUDDY_UNREAD_COUNT = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT);
            COLUMN_ROSTER_BUDDY_AVATAR_HASH = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH);
            COLUMN_ROSTER_BUDDY_DRAFT = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DRAFT);
            COLUMN_ROSTER_BUDDY_LAST_SEEN = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_LAST_SEEN);
            COLUMN_ROSTER_BUDDY_LAST_TYPING = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_LAST_TYPING);
            COLUMN_ROSTER_BUDDY_OPERATION = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_OPERATION);
            isColumnsRead = true;
        }
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

    public int getBuddyAccountDbId() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_ACCOUNT_DB_ID);
    }

    public int getBuddyDbId() {
        return cursor.getInt(COLUMN_ROW_AUTO_ID);
    }

    public String getBuddyId() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_ID);
    }

    public String getBuddyNick() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_NICK);
    }

    public String getBuddyGroup() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_GROUP);
    }

    public int getBuddyGroupId() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_GROUP_ID);
    }

    public boolean getBuddyDialog() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_DIALOG) != 0;
    }

    public int getBuddyStatus() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS);
    }

    public String getBuddyStatusTitle() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_TITLE);
    }

    public String getBuddyStatusMessage() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_MESSAGE);
    }

    public String getBuddyAccountType() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE);
    }

    public int getBuddyAlphabetIndex() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX);
    }

    public int getBuddyUnreadCount() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_UNREAD_COUNT);
    }

    public String getBuddyAvatarHash() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_AVATAR_HASH);
    }

    public String getBuddyDraft() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_DRAFT);
    }

    public long getBuddyLastSeen() {
        return cursor.getLong(COLUMN_ROSTER_BUDDY_LAST_SEEN);
    }

    public long getBuddyLastTyping() {
        return cursor.getLong(COLUMN_ROSTER_BUDDY_LAST_TYPING);
    }

    public int getBuddyOperation() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_OPERATION);
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

    @Override
    public void close() {
        cursor.close();
    }

    public boolean isClosed() {
        return cursor.isClosed();
    }
}
