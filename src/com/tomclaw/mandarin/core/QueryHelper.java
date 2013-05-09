package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/9/13
 * Time: 2:13 PM
 */
public class QueryHelper {

    public static void closeDialog(ContentResolver contentResolver, long buddyDbId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, 0);
        contentResolver.update(Settings.BUDDY_RESOLVER_URI, contentValues,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null);
    }

    public static void insertMessage(ContentResolver contentResolver, long buddyDbId,
                                     int messageType, String cookie, String messageText) {
        // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
        Cursor cursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                GlobalProvider.HISTORY_BUDDY_DB_ID + "='" + buddyDbId + "'" + " AND "
                        + GlobalProvider.HISTORY_MESSAGE_TYPE + "='" + 1 + "'" + " AND "
                        + GlobalProvider.HISTORY_MESSAGE_TIME + ">=" +
                        (System.currentTimeMillis() - 2 * 60 * 1000) + "", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if(cursor.getCount() >= 1) {
            // Moving cursor to the last (and first) position and checking for operation success.
            if(cursor.moveToPosition(cursor.getCount() - 1)) {
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
}
