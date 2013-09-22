package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.IcqStatusUtil;
import com.tomclaw.mandarin.util.StatusUtil;

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

    public static List<AccountRoot> getAccounts(Context context, List<AccountRoot> accountRootList) {
        // Clearing input list.
        accountRootList.clear();
        // Obtain specified account. If exist.
        Cursor cursor = context.getContentResolver().query(Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Obtain necessary column index.
            int bundleColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_BUNDLE);
            int typeColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
            int dbIdColumnIndex = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Iterate all accounts.
            do {
                accountRootList.add(createAccountRoot(context, cursor.getString(typeColumnIndex),
                        cursor.getString(bundleColumnIndex), cursor.getInt(dbIdColumnIndex)));
            } while (cursor.moveToNext()); // Trying to move to position.
        }
        // Closing cursor.
        cursor.close();
        return accountRootList;
    }

    /*public static AccountRoot getAccount(ContentResolver contentResolver, int accountDbId) {
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null, null);
        // Checking for there is at least one account and switching to it.
        if (cursor.moveToFirst()) {
            // Obtain necessary column index.
            int bundleColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_BUNDLE);
            int typeColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
            return createAccountRoot(contentResolver, cursor.getString(typeColumnIndex),
                    cursor.getString(bundleColumnIndex));
        }
        return null;
    }*/

    public static int getAccountDbId(ContentResolver contentResolver, String accountType, String userId)
            throws AccountNotFoundException {
        // Obtain account db id.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountType + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + userId + "'", null, null);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            return accountDbId;
        }
        // Closing cursor.
        cursor.close();
        throw new AccountNotFoundException();
    }

    private static AccountRoot createAccountRoot(Context context, String className,
                                                 String accountRootJson, int accountDbId) {
        try {
            // Creating account root from bundle.
            AccountRoot accountRoot = (AccountRoot) gson.fromJson(accountRootJson,
                    Class.forName(className));
            accountRoot.setContext(context);
            accountRoot.setAccountDbId(accountDbId);
            accountRoot.actualizeStatus();
            return accountRoot;
        } catch (ClassNotFoundException e) {
            Log.d(Settings.LOG_TAG, "No such class found: " + e.getMessage());
        }
        return null;
    }

    public static boolean updateAccount(Context context, AccountRoot accountRoot) {
        ContentResolver contentResolver = context.getContentResolver();
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountRoot.getAccountType() + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + accountRoot.getUserId() + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.moveToFirst()) {
            long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            // We must update account. Name, password, status, bundle.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
            contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
            contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
            contentValues.put(GlobalProvider.ACCOUNT_CONNECTING, accountRoot.isConnecting() ? 1 : 0);
            contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
            // Update query.
            contentResolver.update(Settings.ACCOUNT_RESOLVER_URI, contentValues,
                    GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null);
            return false;
        }
        // Closing cursor.
        cursor.close();
        // No matching account. Creating new account.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType());
        contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
        contentValues.put(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
        contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
        contentValues.put(GlobalProvider.ACCOUNT_CONNECTING, accountRoot.isConnecting() ? 1 : 0);
        contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
        contentResolver.insert(Settings.ACCOUNT_RESOLVER_URI, contentValues);
        // Setting up account db id.
        try {
            accountRoot.setAccountDbId(getAccountDbId(contentResolver, accountRoot.getAccountType(),
                    accountRoot.getUserId()));
            accountRoot.setContext(context);
        } catch (AccountNotFoundException e) {
            // Hey, I'm inserted it 3 lines ago!
            Log.d(Settings.LOG_TAG, "updateAccount method: no accounts after inserting.");
        }
        return true;
    }

    public static boolean updateAccountStatus(ContentResolver contentResolver, AccountRoot accountRoot,
                                              int statusIndex, boolean isConnecting) {
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ACCOUNT_TYPE + "='" + accountRoot.getAccountType() + "'" + " AND "
                        + GlobalProvider.ACCOUNT_USER_ID + "='" + accountRoot.getUserId() + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.moveToFirst()) {
            long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            // We must update account. Status, connecting flag.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
            contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
            // Update query.
            contentResolver.update(Settings.ACCOUNT_RESOLVER_URI, contentValues,
                    GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null);
            return true;
        }
        // Closing cursor.
        cursor.close();
        return false;
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
        // Closing cursor.
        cursor.close();
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

    public static void modifyFavorite(ContentResolver contentResolver, int buddyDbId, boolean isFavorite) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_FAVORITE, isFavorite ? 1 : 0);
        modifyBuddy(contentResolver, buddyDbId, contentValues);
    }

    public static boolean checkDialog(ContentResolver contentResolver, int buddyDbId) {
        boolean dialogFlag = false;
        // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.moveToFirst()) {
             dialogFlag = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DIALOG)) != 0;
            // Closing cursor.
            cursor.close();
        }
        // Closing cursor.
        cursor.close();
        return dialogFlag;
    }

    public static void insertMessage(ContentResolver contentResolver, int buddyDbId, int messageType, String cookie,
                                     String messageText, boolean activateDialog) throws BuddyNotFoundException {
        insertMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
                messageType, cookie, 0, messageText, activateDialog);
    }

    public static void insertMessage(ContentResolver contentResolver, int accountDbId,
                                     int buddyDbId, int messageType, String cookie, long messageTime,
                                     String messageText, boolean activateDialog) {
        Log.d(Settings.LOG_TAG, "insertMessage: type: " + messageType + " message = " + messageText);
        // Checking for time specified.
        if(messageTime == 0) {
            messageTime = System.currentTimeMillis();
        }
        // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
        Cursor cursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                GlobalProvider.HISTORY_BUDDY_DB_ID + "='" + buddyDbId + "'", null, null);
        // Cursor may have no more than only one entry. But we will check one and more.
        if (cursor.getCount() >= 1) {
            // Moving cursor to the last (and first) position and checking for operation success.
            if (cursor.moveToLast()
                    && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE)) == messageType
                    && cursor.getLong(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME)) >=
                    (messageTime - Settings.MESSAGES_COLLAPSE_DELAY)
                    && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_STATE)) != 1) {
                Log.d(Settings.LOG_TAG, "We have cookies!");
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
                contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, 2);
                contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
                // Update query.
                contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues,
                        GlobalProvider.ROW_AUTO_ID + "='" + messageDbId + "'", null);
                // Closing cursor.
                cursor.close();
                // Checking for dialog activate needed.
                if(activateDialog && !checkDialog(contentResolver, buddyDbId)) {
                    modifyDialog(contentResolver, buddyDbId, true);
                }
                return;
            }
        }
        // No matching request message. Insert new message.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, 2);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
        // Closing cursor.
        cursor.close();
        // Checking for dialog activate needed.
        if(activateDialog && !checkDialog(contentResolver, buddyDbId)) {
            modifyDialog(contentResolver, buddyDbId, true);
        }
    }

    public static void insertMessage(ContentResolver contentResolver, int accountDbId, String userId,
                                     int messageType, String cookie, long messageTime,
                                    String messageText, boolean activateDialog)
            throws BuddyNotFoundException {
        // Obtain account db id.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID + "='" + accountDbId + "'" + " AND "
                        + GlobalProvider.ROSTER_BUDDY_ID + "='" + userId + "'", null, null);
        // Cursor may have more than only one entry.
        // TODO: check for at least one buddy present.
        if (cursor.moveToFirst()) {
            final int BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                // Plain message query.
                insertMessage(contentResolver, accountDbId,
                        buddyDbId, messageType, cookie, messageTime, messageText, activateDialog);
            } while(cursor.moveToNext());
            // Closing cursor.
            cursor.close();
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void updateMessage(ContentResolver contentResolver, String cookie, int messageState) {
        // Plain message modify by cookies.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues,
                GlobalProvider.HISTORY_MESSAGE_COOKIE + " LIKE '%" + cookie + "%'", null);
    }

    public static void readMessages(ContentResolver contentResolver, int buddyDbId,
                                    long messageDbIdFirst, long messageDbIdLast) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(GlobalProvider.HISTORY_BUDDY_DB_ID).append("='").append(buddyDbId).append("'")
                .append(" AND ").append(GlobalProvider.ROW_AUTO_ID).append(">=").append(messageDbIdFirst)
                .append(" AND ").append(GlobalProvider.ROW_AUTO_ID).append("<=").append(messageDbIdLast)
                .append(" AND ").append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("=").append(1)
                .append(" AND ").append(GlobalProvider.HISTORY_MESSAGE_READ).append("=").append(0);

        // Obtain unread messages count.
        Cursor cursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                queryBuilder.toString(), null, null);
        // Checking for unread messages.
        if(cursor.getCount() > 0) {
            // Plain messages modify by buddy db id and messages db id.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);

            contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues, queryBuilder.toString(), null);
        } else {
            Log.d(Settings.LOG_TAG, "Marking as read query, but no unread messages found");
        }
    }

    private static void modifyBuddy(ContentResolver contentResolver, int buddyDbId, ContentValues contentValues) {
        contentResolver.update(Settings.BUDDY_RESOLVER_URI, contentValues,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null);
    }

    public static void modifyBuddyStatus(ContentResolver contentResolver, int accountDbId, String buddyId,
                                         int buddyStatusIndex) throws BuddyNotFoundException {
        // Obtain account db id.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID + "='" + accountDbId + "'" + " AND "
                        + GlobalProvider.ROSTER_BUDDY_ID + "='" + buddyId + "'", null, null);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            final int BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                // Plain buddy modify.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, buddyStatusIndex);
                modifyBuddy(contentResolver, buddyDbId, contentValues);
            } while(cursor.moveToNext());
            // Closing cursor.
            cursor.close();
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void updateOrCreateBuddy(ContentResolver contentResolver, int accountDbId, String accountType,
                                           long updateTime, String groupName,
                                           String buddyId, String buddyNick, String buddyStatus) {
        ContentValues buddyValues = new ContentValues();
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE, accountType);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_NICK, buddyNick);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
        // buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, groupDbId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, IcqStatusUtil.getStatusIndex(buddyStatus));
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, 0);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_FAVORITE, 0);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime);
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(GlobalProvider.ROSTER_BUDDY_ID).append("=='").append(buddyId).append("'")
                .append(" AND ")
                .append(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID).append("==").append(accountDbId);
        Cursor buddyCursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                queryBuilder.toString(), null,
                GlobalProvider.ROW_AUTO_ID.concat(" ASC LIMIT 1"));
        if (buddyCursor.moveToFirst()) {
            long buddyDbId = buddyCursor.getLong(buddyCursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            int buddyDialogFlag = buddyCursor.getInt(buddyCursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DIALOG));
            int buddyFavorite = buddyCursor.getInt(buddyCursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_FAVORITE));
            // Update dialog and favorite flags.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, buddyDialogFlag);
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_FAVORITE, buddyFavorite);
            // Update this row.
            queryBuilder.setLength(0);
            queryBuilder.append(GlobalProvider.ROW_AUTO_ID).append("=='").append(buddyDbId).append("'");
            contentResolver.update(Settings.BUDDY_RESOLVER_URI, buddyValues,
                    queryBuilder.toString(), null);
        } else {
            contentResolver.insert(Settings.BUDDY_RESOLVER_URI, buddyValues);
        }
        buddyCursor.close();
    }

    public static void updateOrCreateGroup(ContentResolver contentResolver, int accountDbId, long updateTime,
                                           String groupName, int groupId) {
        StringBuilder queryBuilder = new StringBuilder();
        ContentValues groupValues = new ContentValues();
        groupValues.put(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_NAME, groupName);
        groupValues.put(GlobalProvider.ROSTER_GROUP_ID, groupId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_DEFAULT);
        groupValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, updateTime);
        // Trying to update group
        queryBuilder.append(GlobalProvider.ROSTER_GROUP_ID).append("=='").append(groupId).append("'")
                .append(" AND ")
                .append(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID).append("==").append(accountDbId);
        int groupsModified = contentResolver.update(Settings.GROUP_RESOLVER_URI, groupValues,
                queryBuilder.toString(), null);
        // Checking for there is no such group.
        if (groupsModified == 0) {
            contentResolver.insert(Settings.GROUP_RESOLVER_URI, groupValues);
        }
    }

    public static void moveOutdatedBuddies(ContentResolver contentResolver, Resources resources,
                                           int accountDbId, long updateTime) {
        String recycleString = resources.getString(R.string.recycle);
        StringBuilder queryBuilder = new StringBuilder();
        // Move all deleted buddies to recycle.
        queryBuilder.append(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME).append("!=").append(updateTime)
        .append(" AND ")
        .append(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID).append("==").append(accountDbId);
        Cursor removedCursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                queryBuilder.toString(), null, null);
        if(removedCursor.moveToFirst()) {
            // Checking and creating recycle.
            queryBuilder.setLength(0);
            queryBuilder.append(GlobalProvider.ROSTER_GROUP_TYPE).append("=='")
                    .append(GlobalProvider.GROUP_TYPE_SYSTEM).append("'").append(" AND ")
                    .append(GlobalProvider.ROSTER_GROUP_ID).append("==").append(GlobalProvider.GROUP_ID_RECYCLE);
            Cursor recycleCursor = contentResolver.query(Settings.GROUP_RESOLVER_URI, null,
                    queryBuilder.toString(), null, null);
            if(!recycleCursor.moveToFirst()) {
                ContentValues recycleValues = new ContentValues();
                recycleValues.put(GlobalProvider.ROSTER_GROUP_NAME, recycleString);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, updateTime);
                contentResolver.insert(Settings.GROUP_RESOLVER_URI, recycleValues);
            }
            recycleCursor.close();
            // Move, move, move!
            ContentValues moveValues = new ContentValues();
            moveValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, recycleString);
            moveValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
            moveValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);

            queryBuilder.setLength(0);
            queryBuilder.append(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME).append("!=").append(updateTime)
                    .append(" AND ")
                    .append(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID).append("==").append(accountDbId);
            int movedBuddies = contentResolver.update(Settings.BUDDY_RESOLVER_URI, moveValues,
                    queryBuilder.toString(), null);
            Log.d(Settings.LOG_TAG, "moved to recycle: " + movedBuddies);
        }
        removedCursor.close();
    }

    public static int getBuddyAccountDbId(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        // Obtain specified buddy. If exist.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Checking for there is at least one buddy and switching to it.
        if (cursor.moveToFirst()) {
            // Obtain necessary column index.
            int nickColumnIndex = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            int accountDbId = cursor.getInt(nickColumnIndex);
            // Closing cursor.
            cursor.close();
            return accountDbId;
        }
        throw new BuddyNotFoundException();
    }

    public static String getBuddyNick(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        // Obtain specified buddy. If exist.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Checking for there is at least one buddy and switching to it.
        if (cursor.moveToFirst()) {
            // Obtain necessary column index.
            int nickColumnIndex = cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_NICK);
            String buddyNick = cursor.getString(nickColumnIndex);
            // Closing cursor.
            cursor.close();
            return buddyNick;
        }
        throw new BuddyNotFoundException();
    }

    public static String getAccountName(ContentResolver contentResolver, int accountDbId)
            throws AccountNotFoundException {
        // Obtain specified account. If exist.
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null, null);
        // Checking for there is at least one account and switching to it.
        if (cursor.moveToFirst()) {
            // Obtain necessary column index.
            int nameColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
            String accountName = cursor.getString(nameColumnIndex);
            // Closing cursor.
            cursor.close();
            return accountName;
        }
        throw new AccountNotFoundException();
    }

    public static void clearHistory(ContentResolver contentResolver, int buddyDbId) {
        contentResolver.delete(Settings.HISTORY_RESOLVER_URI,
                GlobalProvider.HISTORY_BUDDY_DB_ID + "='" + buddyDbId + "'", null);
    }

    public static int getMoreActiveDialog(ContentResolver contentResolver) throws BuddyNotFoundException, MessageNotFoundException {
        StringBuilder queryBuilder = new StringBuilder();
        // Query for opened dialogs.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                queryBuilder.append(GlobalProvider.ROSTER_BUDDY_DIALOG).append("='").append(1).append("'").toString(),
                null, null);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            int BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Creating query to history table, contains all messages from all opened dialogs.
            queryBuilder = new StringBuilder();
            do {
                int buddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                queryBuilder.append(GlobalProvider.HISTORY_BUDDY_DB_ID).append("='").append(buddyDbId).append("'");
                if(!cursor.isLast()) {
                    queryBuilder.append(" OR ");
                }
            } while(cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            // Query for the most recent message.
            cursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                    queryBuilder.toString(), null, GlobalProvider.ROW_AUTO_ID + " DESC");
            // Cursor may have more than only one entry. We need only first.
            if (cursor.moveToFirst()) {
                BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
                int moreActiveBuddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                // Closing cursor.
                cursor.close();
                return moreActiveBuddyDbId;
            } else {
                // Closing cursor.
                cursor.close();
                // Really no messages.
                throw new MessageNotFoundException();
            }
        } else {
            // Closing cursor.
            cursor.close();
            // No opened dialogs.
            throw new BuddyNotFoundException();
        }
    }
}
