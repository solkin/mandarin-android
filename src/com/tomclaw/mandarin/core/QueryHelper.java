package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/9/13
 * Time: 2:13 PM
 */
public class QueryHelper {

    public static boolean updateAccount(ContentResolver contentResolver, AccountRoot accountRoot) {
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountRoot.getAccountType() + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + accountRoot.getUserId() + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if(cursor.getCount() >= 1) {
            if(cursor.moveToFirst()) {

            }
        }
        return false;
    }

    public static void modifyDialog(ContentResolver contentResolver, long buddyDbId, boolean isOpened) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        modifyBuddy(contentResolver, buddyDbId, contentValues);
    }

    public static void insertMessage(ContentResolver contentResolver, long buddyDbId,
                                     int messageType, String cookie, String messageText) {
        // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
        Cursor cursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                GlobalProvider.HISTORY_BUDDY_DB_ID + "='" + buddyDbId + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.getCount() >= 1) {
            // Moving cursor to the last (and first) position and checking for operation success.
            if (cursor.moveToLast()
                    && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE)) == messageType
                    && cursor.getLong(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME)) >=
                    (System.currentTimeMillis() - Settings.MESSAGES_COLLAPSE_DELAY)) {
                // We have cookies!
                long messageDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                String cookies = cursor.getString(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_COOKIE));
                cookies += " " + cookie;
                String messagesText = cursor.getString(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT));
                messagesText += "\n" + messageText;
                // Creating content values to update message.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookies);
                contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messagesText);
                // Update query.
                contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues,
                        GlobalProvider.ROW_AUTO_ID + "='" + messageDbId + "'", null);
                return;
            }
        }
        // No matching request message. Insert new message.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_NICK, "Nick name");
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, 1);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, System.currentTimeMillis());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    private static void modifyBuddy(ContentResolver contentResolver, long buddyDbId, ContentValues contentValues) {
        contentResolver.update(Settings.BUDDY_RESOLVER_URI, contentValues,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null);
    }
}
