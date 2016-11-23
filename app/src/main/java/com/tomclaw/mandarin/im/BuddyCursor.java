package com.tomclaw.mandarin.im;

import android.database.Cursor;

import com.tomclaw.mandarin.core.GlobalProvider;

import java.io.Closeable;

/**
 * Created by solkin on 23/03/14.
 * Cursor wrapper for roster table
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
    private static int COLUMN_ROSTER_BUDDY_LAST_MESSAGE_ID;
    private static int COLUMN_ROSTER_BUDDY_YOURS_LAST_READ;
    private static int COLUMN_ROSTER_BUDDY_THEIRS_LAST_DELIVERED;
    private static int COLUMN_ROSTER_BUDDY_THEIRS_LAST_READ;

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
        if (!isColumnsRead && cursor != null) {
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
            COLUMN_ROSTER_BUDDY_LAST_MESSAGE_ID = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_ID);
            COLUMN_ROSTER_BUDDY_YOURS_LAST_READ = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_YOURS_LAST_READ);
            COLUMN_ROSTER_BUDDY_THEIRS_LAST_DELIVERED = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_THEIRS_LAST_DELIVERED);
            COLUMN_ROSTER_BUDDY_THEIRS_LAST_READ = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_THEIRS_LAST_READ);
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

    public int getAccountDbId() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_ACCOUNT_DB_ID);
    }

    public int getDbId() {
        return cursor.getInt(COLUMN_ROW_AUTO_ID);
    }

    public String getBuddyId() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_ID);
    }

    public String getBuddyNick() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_NICK);
    }

    public String getGroup() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_GROUP);
    }

    public int getGroupId() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_GROUP_ID);
    }

    public boolean getDialog() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_DIALOG) != 0;
    }

    public int getStatus() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_STATUS);
    }

    public String getStatusTitle() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_TITLE);
    }

    public String getStatusMessage() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_STATUS_MESSAGE);
    }

    public String getAccountType() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_ACCOUNT_TYPE);
    }

    public int getAlphabetIndex() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_ALPHABET_INDEX);
    }

    public int getUnreadCount() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_UNREAD_COUNT);
    }

    public String getAvatarHash() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_AVATAR_HASH);
    }

    public String getDraft() {
        return cursor.getString(COLUMN_ROSTER_BUDDY_DRAFT);
    }

    public long getLastSeen() {
        return cursor.getLong(COLUMN_ROSTER_BUDDY_LAST_SEEN);
    }

    public long getLastTyping() {
        return cursor.getLong(COLUMN_ROSTER_BUDDY_LAST_TYPING);
    }

    public int getOperation() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_OPERATION);
    }

    public long getLastMessageId() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_LAST_MESSAGE_ID);
    }

    public long getYoursLastRead() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_YOURS_LAST_READ);
    }

    public long getTheirsLastDelivered() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_THEIRS_LAST_DELIVERED);
    }

    public long getTheirsLastRead() {
        return cursor.getInt(COLUMN_ROSTER_BUDDY_THEIRS_LAST_READ);
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
        if (cursor != null) {
            cursor.close();
        }
    }

    public boolean isClosed() {
        return cursor.isClosed();
    }

    public BuddyData toBuddyData() {
        return new BuddyData(getGroupId(), getGroup(), getBuddyId(),
                getBuddyNick(), getStatus(), getStatusTitle(),
                getStatusMessage(), getAvatarHash(), getLastSeen());
    }

    public StrictBuddy toBuddy() {
        return new StrictBuddy(getAccountDbId(), getGroup(), getBuddyId());
    }
}
