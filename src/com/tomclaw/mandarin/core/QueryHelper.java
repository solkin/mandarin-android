package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.im.AccountRoot;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/9/13
 * Time: 2:13 PM
 */
public class QueryHelper {

    private static Gson gson;

    static {
        gson = new Gson();
    }

    public static List<AccountRoot> getAccounts(ContentResolver contentResolver, List<AccountRoot> accountRootList) {
        // Clearing input list.
        accountRootList.clear();
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
        // Cursor may have more than only one entry.
        if (cursor.getCount() >= 1) {
            // Obtain necessary column index.
            int bundleColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_BUNDLE);
            int typeColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
            // Iterate all accounts.
            if (cursor.moveToFirst()) {
                do {
                    try {
                        // Creating account root from bundle.
                        AccountRoot accountRoot = (AccountRoot) gson.fromJson(cursor.getString(bundleColumnIndex),
                                Class.forName(cursor.getString(typeColumnIndex)));
                        accountRootList.add(accountRoot);
                    } catch (ClassNotFoundException e) {
                        Log.d(Settings.LOG_TAG, "No such class found: " + e.getMessage());
                    }
                } while (cursor.moveToNext());// Trying to move to position.
            }
        }
        return accountRootList;
    }

    public static boolean updateAccount(ContentResolver contentResolver, AccountRoot accountRoot) {
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountRoot.getAccountType() + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + accountRoot.getUserId() + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.getCount() >= 1) {
            if (cursor.moveToFirst()) {
                long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                // We must update account. Name, password, status, bundle.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
                contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
                contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
                contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
                // Update query.
                contentResolver.update(Settings.ACCOUNT_RESOLVER_URI, contentValues,
                        GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null);
                return false;
            }
        }
        // No matching account. Creating new account.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType());
        contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
        contentValues.put(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
        contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
        contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
        contentResolver.insert(Settings.ACCOUNT_RESOLVER_URI, contentValues);
        return true;
    }

    public static boolean removeAccount(ContentResolver contentResolver, String accountType, String userId) {
        // Obtain account db id.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountType + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + userId + "'", null, null);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            do {
                long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                // Removing roster groups.
                contentResolver.delete(Settings.GROUP_RESOLVER_URI,
                        GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID + "=" + accountDbId, null);
                // Removing roster buddies.
                contentResolver.delete(Settings.BUDDY_RESOLVER_URI,
                        GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID + "=" + accountDbId, null);
                // Removing all the history.
                contentResolver.delete(Settings.HISTORY_RESOLVER_URI,
                        GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID + "=" + accountDbId, null);
            } while (cursor.moveToNext());
        }
        // And remove account.
        return contentResolver.delete(Settings.ACCOUNT_RESOLVER_URI, GlobalProvider.ACCOUNT_TYPE + "='"
                + accountType + "'" + " AND " + GlobalProvider.ACCOUNT_USER_ID + "='"
                + userId + "'", null) != 0;
    }

    public static void modifyDialog(ContentResolver contentResolver, int buddyDbId, boolean isOpened) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        modifyBuddy(contentResolver, buddyDbId, contentValues);
    }

    public static void insertMessage(ContentResolver contentResolver, int accountDbId, int buddyDbId,
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
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, 1);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, System.currentTimeMillis());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    private static void modifyBuddy(ContentResolver contentResolver, int buddyDbId, ContentValues contentValues) {
        contentResolver.update(Settings.BUDDY_RESOLVER_URI, contentValues,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null);
    }
}
