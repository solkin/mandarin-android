package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.icq.IcqStatusUtil;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.StatusUtil;
import com.tomclaw.mandarin.util.StringUtil;

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

    public static int getAccountDbId(ContentResolver contentResolver, String accountType, String userId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ACCOUNT_TYPE, accountType)
                .and().columnEquals(GlobalProvider.ACCOUNT_USER_ID, userId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
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
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified account. If exist.
        queryBuilder.columnEquals(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType())
                .and().columnEquals(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
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
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
            queryBuilder.update(contentResolver, contentValues, Settings.ACCOUNT_RESOLVER_URI);
            if(accountRoot.getStatusIndex() == StatusUtil.STATUS_OFFLINE) {
                // Update status for account buddies to unknown.
                contentValues = new ContentValues();
                contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
                // Update query.
                queryBuilder.recycle();
                queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                queryBuilder.update(contentResolver, contentValues, Settings.BUDDY_RESOLVER_URI);
            }
            return true;
        }
        // Closing cursor.
        cursor.close();
        return false;
    }

    public static void insertAccount(Context context, AccountRoot accountRoot) {
        ContentResolver contentResolver = context.getContentResolver();
        // Creating new account.
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
    }

    public static boolean updateAccountStatus(ContentResolver contentResolver, AccountRoot accountRoot) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType())
                .and().columnEquals(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        // Obtain specified account. If exist.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
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
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
            queryBuilder.update(contentResolver, contentValues, Settings.ACCOUNT_RESOLVER_URI);
            return true;
        }
        // Closing cursor.
        cursor.close();
        return false;
    }

    public static boolean removeAccount(ContentResolver contentResolver, int accountDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            do {
                QueryBuilder removeBuilder = new QueryBuilder();
                removeBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
                // Removing roster groups.
                removeBuilder.delete(contentResolver, Settings.GROUP_RESOLVER_URI);
                // Removing roster buddies.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(contentResolver, Settings.BUDDY_RESOLVER_URI);
                // Removing all the history.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(contentResolver, Settings.HISTORY_RESOLVER_URI);
                // Removing all pending requests.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(contentResolver, Settings.REQUEST_RESOLVER_URI);
            } while (cursor.moveToNext());
        }
        // Closing cursor.
        cursor.close();
        // And remove account.
        return queryBuilder.delete(contentResolver, Settings.ACCOUNT_RESOLVER_URI) != 0;
    }

    public static void modifyDialog(ContentResolver contentResolver, int buddyDbId, boolean isOpened) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        modifyBuddy(contentResolver, buddyDbId, contentValues);
    }

    public static boolean checkDialog(ContentResolver contentResolver, int buddyDbId) {
        boolean dialogFlag = false;
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
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

    public static void insertMessage(Context context, int buddyDbId, int messageType, String cookie,
                                     String messageText, boolean activateDialog) throws BuddyNotFoundException {
        insertMessage(context, getBuddyAccountDbId(context.getContentResolver(), buddyDbId), buddyDbId,
                messageType, cookie, 0, messageText, activateDialog);
    }

    public static void insertMessage(Context context, int accountDbId,
                                     int buddyDbId, int messageType, String cookie, long messageTime,
                                     String messageText, boolean activateDialog) {
        ContentResolver contentResolver = context.getContentResolver();
        Log.d(Settings.LOG_TAG, "insertMessage: type: " + messageType + " message = " + messageText);
        // Checking for dialog activate needed.
        if(activateDialog && !checkDialog(contentResolver, buddyDbId)) {
            modifyDialog(contentResolver, buddyDbId, true);
        }
        // Checking for time specified.
        if(messageTime == 0) {
            messageTime = System.currentTimeMillis();
        }
        boolean isCollapseMessages = context.getSharedPreferences(context.getPackageName() + "_preferences",
                Context.MODE_MULTI_PROCESS).getBoolean(
                context.getResources().getString(R.string.pref_collapse_messages),
                context.getResources().getBoolean(R.bool.pref_collapse_messages_default));

        if(isCollapseMessages) {
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
            // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
            Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
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
                    contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);
                    // Update query.
                    queryBuilder.recycle();
                    queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, messageDbId);
                    queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
                    // Closing cursor.
                    cursor.close();
                    return;
                }
            }
            // Closing cursor.
            cursor.close();
        }
        // No matching request message. Insert new message.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, 2);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    public static void insertMessage(Context context, int accountDbId, String userId,
                                     int messageType, String cookie, long messageTime,
                                    String messageText, boolean activateDialog)
            throws BuddyNotFoundException {
        ContentResolver contentResolver = context.getContentResolver();
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, userId);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        // TODO: check for at least one buddy present.
        if (cursor.moveToFirst()) {
            final int BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                // Plain message query.
                insertMessage(context, accountDbId,
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

    public static void updateMessageState(ContentResolver contentResolver, String cookie, int messageState) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        // Plain message modify by cookies.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void readMessages(ContentResolver contentResolver, int buddyDbId,
                                    long messageDbIdFirst, long messageDbIdLast) {

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                .and().moreOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdFirst)
                .and().lessOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdLast)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, 1)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0);

        // Obtain unread messages count.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
        // Checking for unread messages.
        if(cursor.getCount() > 0) {
            // Plain messages modify by buddy db id and messages db id.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);
            contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);

            queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
        } else {
            Log.d(Settings.LOG_TAG, "Marking as read query, but no unread messages found");
        }
    }

    private static void modifyBuddy(ContentResolver contentResolver, int buddyDbId, ContentValues contentValues) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        queryBuilder.update(contentResolver, contentValues, Settings.BUDDY_RESOLVER_URI);
    }

    public static void modifyBuddyStatus(ContentResolver contentResolver, int accountDbId, String buddyId,
                                         int buddyStatusIndex) throws BuddyNotFoundException {
        // Obtain account db id.
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
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
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, StringUtil.getAlphabetIndex(buddyNick));
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId).and()
                .columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        queryBuilder.ascending(GlobalProvider.ROW_AUTO_ID).limit(1);
        Cursor buddyCursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        if (buddyCursor.moveToFirst()) {
            long buddyDbId = buddyCursor.getLong(buddyCursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            int buddyDialogFlag = buddyCursor.getInt(buddyCursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_DIALOG));
            // Update dialog and favorite flags.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, buddyDialogFlag);
            // Update this row.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
            queryBuilder.update(contentResolver, buddyValues, Settings.BUDDY_RESOLVER_URI);
        } else {
            contentResolver.insert(Settings.BUDDY_RESOLVER_URI, buddyValues);
        }
        buddyCursor.close();
    }

    public static void updateOrCreateGroup(ContentResolver contentResolver, int accountDbId, long updateTime,
                                           String groupName, int groupId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        ContentValues groupValues = new ContentValues();
        groupValues.put(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_NAME, groupName);
        groupValues.put(GlobalProvider.ROSTER_GROUP_ID, groupId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_DEFAULT);
        groupValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, updateTime);
        // Trying to update group
        queryBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_ID, groupId).and()
                .columnEquals(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
        int groupsModified = queryBuilder.update(contentResolver, groupValues, Settings.GROUP_RESOLVER_URI);
        // Checking for there is no such group.
        if (groupsModified == 0) {
            contentResolver.insert(Settings.GROUP_RESOLVER_URI, groupValues);
        }
    }

    public static void moveOutdatedBuddies(ContentResolver contentResolver, Resources resources,
                                           int accountDbId, long updateTime) {
        String recycleString = resources.getString(R.string.recycle);
        QueryBuilder queryBuilder = new QueryBuilder();
        // Move all deleted buddies to recycle.
        queryBuilder.columnNotEquals(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        Cursor removedCursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        if(removedCursor.moveToFirst()) {
            // Checking and creating recycle.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM)
                    .and().columnEquals(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
            Cursor recycleCursor = queryBuilder.query(contentResolver, Settings.GROUP_RESOLVER_URI);
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

            queryBuilder.recycle();
            queryBuilder.columnNotEquals(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime)
                    .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);

            int movedBuddies = queryBuilder.update(contentResolver, moveValues, Settings.BUDDY_RESOLVER_URI);
            Log.d(Settings.LOG_TAG, "moved to recycle: " + movedBuddies);
        }
        removedCursor.close();
    }

    public static int getBuddyAccountDbId(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        // Obtain specified buddy. If exist.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
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
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        // Obtain specified buddy. If exist.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
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
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain specified account. If exist.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
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
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        queryBuilder.delete(contentResolver, Settings.HISTORY_RESOLVER_URI);
    }

    public static int getMoreActiveDialog(ContentResolver contentResolver) throws BuddyNotFoundException, MessageNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Query for opened dialogs.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_DIALOG, 1);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            int BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Creating query to history table, contains all messages from all opened dialogs.
            queryBuilder.recycle();
            do {
                int buddyDbId = cursor.getInt(BUDDY_DB_ID_COLUMN);
                queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                if(!cursor.isLast()) {
                    queryBuilder.or();
                }
            } while(cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            // Query for the most recent message.
            queryBuilder.descending(GlobalProvider.ROW_AUTO_ID);
            cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
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
