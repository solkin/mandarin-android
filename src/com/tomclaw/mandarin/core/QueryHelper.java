package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.AccountAlreadyExistsException;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/9/13
 * Time: 2:13 PM
 */
public class QueryHelper {

    public static List<AccountRoot> getAccounts(Context context, List<AccountRoot> accountRootList) {
        // Clearing input list.
        accountRootList.clear();
        // Obtain specified account. If exist.
        Cursor cursor = context.getContentResolver().query(Settings.ACCOUNT_RESOLVER_URI, null, null, null, null);
        // Cursor may be null, so we must check it.
        if (cursor != null) {
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
        }
        return accountRootList;
    }

    public static AccountRoot getAccount(Context context, int accountDbId) {
        AccountRoot accountRoot = null;
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(context.getContentResolver(), Settings.ACCOUNT_RESOLVER_URI);
        // Cursor may be null, so we must check it.
        if (cursor != null) {
            // Cursor may have more than only one entry.
            if (cursor.moveToFirst()) {
                // Obtain necessary column index.
                int bundleColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_BUNDLE);
                int typeColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE);
                int dbIdColumnIndex = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
                // Iterate all accounts.
                accountRoot = createAccountRoot(context, cursor.getString(typeColumnIndex),
                        cursor.getString(bundleColumnIndex), cursor.getInt(dbIdColumnIndex));
            }
            // Closing cursor.
            cursor.close();
        }
        return accountRoot;
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
            AccountRoot accountRoot = (AccountRoot) GsonSingleton.getInstance().fromJson(accountRootJson,
                    Class.forName(className));
            accountRoot.setContext(context);
            accountRoot.setAccountDbId(accountDbId);
            accountRoot.actualizeStatus();
            return accountRoot;
        } catch (ClassNotFoundException e) {
            Logger.log("No such class found: " + e.getMessage());
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
        // Cursor may have only one entry.
        if (cursor.moveToFirst()) {
            long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            // We must update account. Name, password, status, bundle.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
            contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
            contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
            contentValues.put(GlobalProvider.ACCOUNT_STATUS_TITLE, accountRoot.getStatusTitle());
            contentValues.put(GlobalProvider.ACCOUNT_STATUS_MESSAGE, accountRoot.getStatusMessage());
            // Checking for no user icon now, so, we must reset avatar hash.
            if (TextUtils.isEmpty(accountRoot.getAvatarHash())) {
                contentValues.putNull(GlobalProvider.ACCOUNT_AVATAR_HASH);
            } else {
                contentValues.put(GlobalProvider.ACCOUNT_AVATAR_HASH, accountRoot.getAvatarHash());
            }
            contentValues.put(GlobalProvider.ACCOUNT_CONNECTING, accountRoot.isConnecting() ? 1 : 0);
            contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, GsonSingleton.getInstance().toJson(accountRoot));
            // Update query.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
            queryBuilder.update(contentResolver, contentValues, Settings.ACCOUNT_RESOLVER_URI);
            if (accountRoot.isOffline()) {
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

    public static boolean checkAccount(ContentResolver contentResolver, String accountType, String userId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified account. If exist.
        queryBuilder.columnEquals(GlobalProvider.ACCOUNT_TYPE, accountType)
                .and().columnEquals(GlobalProvider.ACCOUNT_USER_ID, userId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
        // Cursor may have one entry or nothing.
        boolean accountExists = cursor.moveToFirst();
        // Closing cursor.
        cursor.close();
        return accountExists;
    }

    public static boolean isAccountActive(ContentResolver contentResolver, int accountDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId)
                .and().columnNotEquals(GlobalProvider.ACCOUNT_STATUS, StatusUtil.STATUS_OFFLINE)
                .and().columnEquals(GlobalProvider.ACCOUNT_CONNECTING, 0);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
        // Checking for condition is satisfied.
        boolean accountActive = cursor.moveToFirst();
        cursor.close();
        // Closing cursor.
        return accountActive;
    }

    public static Cursor getActiveAccounts(ContentResolver contentResolver) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnNotEquals(GlobalProvider.ACCOUNT_STATUS, StatusUtil.STATUS_OFFLINE)
                .and().columnEquals(GlobalProvider.ACCOUNT_CONNECTING, 0);
        // Not so good decision to return simple cursor!
        return queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
    }

    public static int getAccountsCount(ContentResolver contentResolver) {
        QueryBuilder queryBuilder = new QueryBuilder();
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
            return cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Insert account into database and update it's account db id and context.
     *
     * @param context     - context for account root
     * @param accountRoot - account root to be inserted into database
     * @return account db id.
     * @throws AccountNotFoundException
     */
    public static int insertAccount(Context context, AccountRoot accountRoot)
            throws AccountNotFoundException, AccountAlreadyExistsException {
        if (checkAccount(context.getContentResolver(),
                accountRoot.getAccountType(), accountRoot.getUserId())) {
            throw new AccountAlreadyExistsException();
        }
        ContentResolver contentResolver = context.getContentResolver();
        // Creating new account.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType());
        contentValues.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
        contentValues.put(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        contentValues.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
        contentValues.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
        contentValues.put(GlobalProvider.ACCOUNT_STATUS_TITLE, accountRoot.getStatusTitle());
        contentValues.put(GlobalProvider.ACCOUNT_STATUS_MESSAGE, accountRoot.getStatusMessage());
        contentValues.put(GlobalProvider.ACCOUNT_CONNECTING, accountRoot.isConnecting() ? 1 : 0);
        contentValues.put(GlobalProvider.ACCOUNT_BUNDLE, GsonSingleton.getInstance().toJson(accountRoot));
        contentResolver.insert(Settings.ACCOUNT_RESOLVER_URI, contentValues);
        // Setting up account db id.
        accountRoot.setAccountDbId(getAccountDbId(contentResolver, accountRoot.getAccountType(),
                accountRoot.getUserId()));
        accountRoot.setContext(context);
        return accountRoot.getAccountDbId();
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

    public static void updateAccountAvatar(ContentResolver contentResolver, int accountDbId, String avatarHash) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ACCOUNT_AVATAR_HASH, avatarHash);
        modifyAccount(contentResolver, accountDbId, contentValues);
    }

    public static String getAccountAvatarHash(ContentResolver contentResolver, int accountDbId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
            if (cursor != null && cursor.moveToFirst()) {
                int avatarHashColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_AVATAR_HASH);
                return cursor.getString(avatarHashColumnIndex);
            }
        } finally {
            // Closing cursor.
            if (cursor != null) {
                cursor.close();
            }
        }
        throw new AccountNotFoundException();
    }

    private static void modifyAccount(ContentResolver contentResolver, int accountDbId, ContentValues contentValues) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        queryBuilder.update(contentResolver, contentValues, Settings.ACCOUNT_RESOLVER_URI);
    }

    public static void modifyBuddyDraft(ContentResolver contentResolver, int buddyDbId, String buddyDraft) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DRAFT, buddyDraft);
        modifyBuddies(contentResolver, Collections.singleton(buddyDbId), contentValues);
    }

    public static void modifyDialog(ContentResolver contentResolver, int buddyDbId, boolean isOpened) {
        modifyDialogs(contentResolver, Collections.singleton(buddyDbId), isOpened);
    }

    public static void modifyDialog(ContentResolver contentResolver, int buddyDbId,
                                    boolean isOpened, long lastMessageTime) {
        modifyDialogs(contentResolver, Collections.singleton(buddyDbId), isOpened, lastMessageTime);
    }

    public static void modifyDialogs(ContentResolver contentResolver, Collection<Integer> buddyDbIds, boolean isOpened) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        modifyBuddies(contentResolver, buddyDbIds, contentValues);
    }

    public static void modifyDialogs(ContentResolver contentResolver, Collection<Integer> buddyDbIds,
                                     boolean isOpened, long lastMessageTime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME, lastMessageTime);
        modifyBuddies(contentResolver, buddyDbIds, contentValues);
    }

    public static void modifyOperation(ContentResolver contentResolver, int buddyDbId, int operation) {
        modifyOperation(contentResolver, Collections.singleton(buddyDbId), operation);
    }

    public static void modifyOperation(ContentResolver contentResolver, Collection<Integer> buddyDbIds, int operation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, operation);
        modifyBuddies(contentResolver, buddyDbIds, contentValues);
    }

    public static void modifyBuddyNick(ContentResolver contentResolver, int buddyDbId,
                                       String buddyNick, boolean isStartOperation) {
        modifyBuddyNick(contentResolver, Collections.singleton(buddyDbId), buddyNick, isStartOperation);
    }

    public static void modifyBuddyNick(ContentResolver contentResolver, Collection<Integer> buddyDbIds,
                                       String buddyNick, boolean isStartOperation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_NICK, buddyNick);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, StringUtil.getAlphabetIndex(buddyNick));
        contentValues.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, buddyNick.toUpperCase());
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, isStartOperation ?
                GlobalProvider.ROSTER_BUDDY_OPERATION_RENAME : GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
        modifyBuddies(contentResolver, buddyDbIds, contentValues);
    }

    public static void insertMessage(ContentResolver contentResolver, boolean isCollapseMessages, int buddyDbId,
                                     int messageType, String cookie, String messageText)
            throws BuddyNotFoundException {
        insertTextMessage(contentResolver, isCollapseMessages, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
                messageType, GlobalProvider.HISTORY_MESSAGE_STATE_SENDING, cookie, 0, messageText);
    }

    public static void insertTextMessage(ContentResolver contentResolver, boolean isCollapseMessages,
                                         int accountDbId, int buddyDbId, int messageType, int messageState, String cookie,
                                         long messageTime, String messageText) {
        Logger.log("insertTextMessage: type: " + messageType + " message = " + messageText);
        // Checking for time specified.
        if (messageTime == 0) {
            messageTime = System.currentTimeMillis();
        }
        // Update last message time and make dialog opened.
        modifyDialog(contentResolver, buddyDbId, true, messageTime);
        // Collapse text messages only.
        if (isCollapseMessages) {
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                    .ascending(GlobalProvider.ROW_AUTO_ID);
            // Obtaining cursor with message to such buddy, of such type and not later, than two minutes.
            Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
            // Cursor may have no more than only one entry. But we will check one and more.
            if (cursor.getCount() >= 1) {
                // Moving cursor to the last (and first) position and checking for operation success.
                if (cursor.moveToLast()
                        && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE)) == messageType
                        && cursor.getLong(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME)) >=
                        (messageTime - Settings.MESSAGES_COLLAPSE_DELAY)
                        && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_STATE)) != 1
                        && cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TYPE)) ==
                        GlobalProvider.HISTORY_CONTENT_TYPE_TEXT) {
                    Logger.log("We have cookies!");
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
                    contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
                    contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
                    contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);
                    contentValues.put(GlobalProvider.HISTORY_SEARCH_FIELD, messagesText.toUpperCase());
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
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentValues.put(GlobalProvider.HISTORY_SEARCH_FIELD, messageText.toUpperCase());
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);
        contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    public static void insertOutgoingFileMessage(ContentResolver contentResolver, int buddyDbId, String cookie,
                                                 Uri uri, String name, int contentType, long contentSize,
                                                 String previewHash, String contentTag)
            throws BuddyNotFoundException {
        insertFileMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
                GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, GlobalProvider.HISTORY_MESSAGE_STATE_SENDING,
                cookie, 0, "", contentType, contentSize, GlobalProvider.HISTORY_CONTENT_STATE_WAITING, uri.toString(),
                name, previewHash, contentTag);
    }

    public static void insertIncomingFileMessage(ContentResolver contentResolver, int buddyDbId, String cookie,
                                                 long time, String originalMessage, Uri uri, String name, int contentType,
                                                 long contentSize, String previewHash, String contentTag) throws BuddyNotFoundException {
        insertFileMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, GlobalProvider.HISTORY_MESSAGE_STATE_UNDETERMINED,
                cookie, time, originalMessage, contentType, contentSize, GlobalProvider.HISTORY_CONTENT_STATE_WAITING,
                uri.toString(), name, previewHash, contentTag);
    }

    public static void insertFileMessage(ContentResolver contentResolver, int accountDbId, int buddyDbId,
                                         int messageType, int messageState, String cookie, long messageTime,
                                         String messageText, int contentType, long contentSize, int contentState,
                                         String contentUri, String contentName, String previewHash, String contentTag) {
        // Checking for time specified.
        if (messageTime == 0) {
            messageTime = System.currentTimeMillis();
        }
        // Update last message time and make dialog opened.
        modifyDialog(contentResolver, buddyDbId, true, messageTime);
        // No matching request message. Insert new message.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
        contentValues.put(GlobalProvider.HISTORY_SEARCH_FIELD, messageText.toUpperCase());
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, contentType);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_SIZE, contentSize);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, contentState);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_URI, contentUri);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_NAME, contentName);
        contentValues.put(GlobalProvider.HISTORY_PREVIEW_HASH, previewHash);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TAG, contentTag);
        // Try to modify message or create it.
        if (modifyFile(contentResolver, contentValues, messageType, cookie) == 0) {
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
            contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);
            contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
        }
    }

    public static void insertMessage(ContentResolver contentResolver, boolean isCollapseMessages,
                                     int accountDbId, String buddyId, int messageType, int messageState,
                                     String cookie, long messageTime, String messageText)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                .descending(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Insert message only for buddy with latest message time.
            int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Plain message query.
            insertTextMessage(contentResolver, isCollapseMessages, accountDbId, buddyDbId, messageType, messageState,
                    cookie, messageTime, messageText);
            // Closing cursor.
            cursor.close();
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    /**
     * Will append some more cookie to message.
     *
     * @param contentResolver - content resolver
     * @param cookie          - cookie of message to be updated
     * @param cookiesToAdd    - appending cookies
     */
    public static void addMessageCookie(ContentResolver contentResolver, String cookie, String... cookiesToAdd) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
            if (cursor != null && cursor.moveToFirst()) {
                String cookies = cursor.getString(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_COOKIE));
                for (String cookieAdd : cookiesToAdd) {
                    cookies += " " + cookieAdd;
                }
                // Plain message modify by cookies.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookies);
                queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void updateMessageState(ContentResolver contentResolver, int messageState, String... cookies) {
        QueryBuilder queryBuilder = new QueryBuilder();
        boolean isNotFirstCookie = false;
        queryBuilder.startComplexExpression();
        for (String cookie : cookies) {
            if (TextUtils.isEmpty(cookie)) {
                continue;
            }
            if (isNotFirstCookie) {
                queryBuilder.or();
            } else {
                isNotFirstCookie = true;
            }
            queryBuilder.like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        }
        queryBuilder.finishComplexExpression();
        if (!isNotFirstCookie) {
            // No one cookie appended.
            return;
        }
        // If this is not unknown or error state, we will update only incrementing state.
        if (messageState > GlobalProvider.HISTORY_MESSAGE_STATE_ERROR) {
            queryBuilder.and().less(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        }
        // Plain message modify by cookies.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_STATE, messageState);
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    private static int modifyFile(ContentResolver contentResolver, ContentValues contentValues, int messageType, String cookie) {
        // Plain message modify by cookies.
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType)
                .and().like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        return queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateFileState(ContentResolver contentResolver, int state, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        int modified = modifyFile(contentResolver, contentValues, messageType, cookie);
        Logger.log("modified: " + modified);
    }

    public static void updateFileSize(ContentResolver contentResolver, long size, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_SIZE, size);
        modifyFile(contentResolver, contentValues, messageType, cookie);
    }

    public static void updateFileProgress(ContentResolver contentResolver, int progress, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_PROGRESS, progress);
        modifyFile(contentResolver, contentValues, messageType, cookie);
    }

    public static void updateFileStateAndText(ContentResolver contentResolver, int state,
                                              String text, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, text);
        modifyFile(contentResolver, contentValues, messageType, cookie);
    }

    public static void revertFileToMessage(ContentResolver contentResolver, int messageType,
                                           String originalMessage, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, originalMessage);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_SIZE, 0);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, GlobalProvider.HISTORY_CONTENT_STATE_STABLE);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_PROGRESS, 0);
        contentValues.put(GlobalProvider.HISTORY_CONTENT_URI, "");
        contentValues.put(GlobalProvider.HISTORY_CONTENT_NAME, "");
        contentValues.put(GlobalProvider.HISTORY_PREVIEW_HASH, "");
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TAG, "");
        modifyFile(contentResolver, contentValues, messageType, cookie);
    }

    public static void updateFileStateAndHash(ContentResolver contentResolver, int state,
                                              String hash, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        contentValues.put(GlobalProvider.HISTORY_PREVIEW_HASH, hash);
        modifyFile(contentResolver, contentValues, messageType, cookie);
    }

    public static int getFileState(ContentResolver contentResolver, int messageType, String cookie)
            throws MessageNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType)
                .and().like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_STATE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        throw new MessageNotFoundException();
    }

    public static void readAllMessages(ContentResolver contentResolver) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);

        // Plain messages modify by type, read and shown state.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);

        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void readAllMessages(ContentResolver contentResolver, Collection<Integer> buddyDbIds) {
        // Check for no buddies.
        if (buddyDbIds.isEmpty()) {
            return;
        }
        // Plain messages modify by type, read and shown state.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.startComplexExpression();
        boolean isFirst = true;
        for (int buddyDbId : buddyDbIds) {
            if (isFirst) {
                isFirst = false;
            } else {
                queryBuilder.or();
            }
            queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        }
        queryBuilder.finishComplexExpression()
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);

        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void readMessages(ContentResolver contentResolver, int buddyDbId,
                                    long messageDbIdFirst, long messageDbIdLast) {

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                .and().moreOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdFirst)
                .and().lessOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdLast)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);

        // Plain messages modify by buddy db id and messages db id.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);

        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void fastReadMessages(ContentResolver contentResolver, int buddyDbId,
                                        long messageDbIdFirst, long messageDbIdLast) {

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                .and().moreOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdFirst)
                .and().lessOrEquals(GlobalProvider.ROW_AUTO_ID, messageDbIdLast)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);

        // Plain messages modify by buddy db id and messages db id.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 1);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 0);

        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void removeMessages(ContentResolver contentResolver, Collection<Long> messageIds) {
        messagesByIds(messageIds).delete(contentResolver, Settings.HISTORY_RESOLVER_URI);
    }

    public static boolean isIncomingMessagesPresent(ContentResolver contentResolver, Collection<Long> messageIds) {
        QueryBuilder queryBuilder = messagesByIds(messageIds);
        queryBuilder.and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
        return cursor.getCount() > 0;
    }

    public static void unreadMessages(ContentResolver contentResolver, Collection<Long> messageIds) {
        QueryBuilder queryBuilder = messagesByIds(messageIds);
        queryBuilder.and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING);
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);
        // Mark specified messages as unread.
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateMessage(ContentResolver contentResolver, Collection<Long> messageIds) {
        QueryBuilder queryBuilder = messagesByIds(messageIds);
        queryBuilder.and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING);
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_READ, 0);
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);
        // Mark specified messages as unread.
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    private static QueryBuilder messagesByIds(Collection<Long> messageIds) {
        QueryBuilder queryBuilder = new QueryBuilder();
        boolean isMultiple = false;
        for (long messageId : messageIds) {
            if (isMultiple) {
                queryBuilder.or();
            } else {
                isMultiple = true;
            }
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, messageId);
        }
        return queryBuilder;
    }

    private static void modifyBuddy(ContentResolver contentResolver, int buddyDbId, ContentValues contentValues) {
        modifyBuddies(contentResolver, Collections.singleton(buddyDbId), contentValues);
    }

    private static void modifyBuddies(ContentResolver contentResolver, Collection<Integer> buddyDbIds, ContentValues contentValues) {
        QueryBuilder queryBuilder = new QueryBuilder();
        boolean isFirst = true;
        for (int buddyDbId : buddyDbIds) {
            if (isFirst) {
                isFirst = false;
            } else {
                queryBuilder.or();
            }
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        }
        queryBuilder.update(contentResolver, contentValues, Settings.BUDDY_RESOLVER_URI);
    }

    public static void modifyBuddyAvatar(ContentResolver contentResolver, int accountDbId, String buddyId,
                                         String avatarHash) throws BuddyNotFoundException {
        // Obtain buddy db id.
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                // String dbAvatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH));
                // Plain buddy modify.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH, avatarHash);
                modifyBuddy(contentResolver, buddyDbId, contentValues);
                /* TODO: think about this.
                if(!TextUtils.equals(dbAvatarHash, avatarHash)) {
                    // Avatar changed or removed. No need for previous bitmap in cache.
                    BitmapCache.getInstance().removeBitmap(dbAvatarHash);
                }*/
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void modifyBuddyStatus(ContentResolver contentResolver, int accountDbId, String buddyId,
                                         int buddyStatusIndex, String buddyStatusTitle, String buddyStatusMessage,
                                         String buddyIcon, long lastSeen) throws BuddyNotFoundException {
        // Plain buddy modify.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, buddyStatusIndex);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE, buddyStatusTitle);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE, buddyStatusMessage);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_SEEN, lastSeen);
        String avatarHash;
        // Obtain buddy db id.
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                avatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH));
                // Checking for no buddy icon now, so, we must reset avatar hash.
                if (TextUtils.isEmpty(buddyIcon) && !TextUtils.isEmpty(avatarHash)) {
                    contentValues.putNull(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH);
                }
                modifyBuddy(contentResolver, buddyDbId, contentValues);
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            // There are may be a lot of buddies in lots of groups, but this is the same buddy with the save avatar.
            if (!TextUtils.isEmpty(buddyIcon) && !TextUtils.equals(avatarHash, HttpUtil.getUrlHash(buddyIcon))) {
                // Avatar is ready.
                RequestHelper.requestBuddyAvatar(contentResolver, accountDbId, buddyId, buddyIcon);
            }
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void modifyBuddyTyping(ContentResolver contentResolver, int accountDbId, String buddyId,
                                         boolean isTyping) throws BuddyNotFoundException {
        // Plain buddy modify.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_TYPING, isTyping ? System.currentTimeMillis() : 0);
        // Obtain buddy db id.
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Cycling all the identical buddies in different groups.
            do {
                int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                modifyBuddy(contentResolver, buddyDbId, contentValues);
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void replaceOrCreateBuddy(ContentResolver contentResolver, int accountDbId, String accountType,
                                            long updateTime, int groupId, String groupName, String buddyId,
                                            String buddyNick, String avatarHash) {
        int statusIndex = StatusUtil.STATUS_OFFLINE;
        String statusTitle = StatusUtil.getStatusTitle(accountType, statusIndex);
        String statusMessage = "";
        long lastSeen = -1;

        ContentValues buddyValues = new ContentValues();
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE, accountType);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_NICK, buddyNick);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, groupId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, statusIndex);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE, statusTitle);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE, statusMessage);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, 0);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, StringUtil.getAlphabetIndex(buddyNick));
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, buddyNick.toUpperCase());
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_LAST_SEEN, lastSeen);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH, avatarHash);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_ADD);

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        queryBuilder.ascending(GlobalProvider.ROW_AUTO_ID).limit(1);
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(contentResolver, queryBuilder);
            long buddyDbId = buddyCursor.getBuddyDbId();
            boolean buddyDialogFlag = buddyCursor.getBuddyDialog();
            // Update dialog flag.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, buddyDialogFlag ? 1 : 0);
            // Update this row.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
            queryBuilder.update(contentResolver, buddyValues, Settings.BUDDY_RESOLVER_URI);
        } catch (BuddyNotFoundException ignored) {
            contentResolver.insert(Settings.BUDDY_RESOLVER_URI, buddyValues);
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
    }

    public static void updateOrCreateBuddy(ContentResolver contentResolver, int accountDbId, String accountType,
                                           long updateTime, int groupId, String groupName, String buddyId,
                                           String buddyNick, int statusIndex, String statusTitle,
                                           String statusMessage, String buddyIcon, long lastSeen) {
        updateOrCreateBuddy(ContentResolverLayer.getInstance(contentResolver), accountDbId,
                accountType, updateTime, groupId, groupName, buddyId, buddyNick,
                statusIndex, statusTitle, statusMessage, buddyIcon, lastSeen);
    }

    public static void updateOrCreateBuddy(SQLiteDatabase sqLiteDatabase, int accountDbId, String accountType,
                                           long updateTime, int groupId, String groupName, String buddyId,
                                           String buddyNick, int statusIndex, String statusTitle,
                                           String statusMessage, String buddyIcon, long lastSeen) {
        updateOrCreateBuddy(SQLiteDatabaseLayer.getInstance(sqLiteDatabase), accountDbId,
                accountType, updateTime, groupId, groupName, buddyId, buddyNick,
                statusIndex, statusTitle, statusMessage, buddyIcon, lastSeen);
    }

    public static void updateOrCreateBuddy(DatabaseLayer databaseLayer, int accountDbId, String accountType,
                                           long updateTime, int groupId, String groupName, String buddyId,
                                           String buddyNick, int statusIndex, String statusTitle,
                                           String statusMessage, String buddyIcon, long lastSeen) {
        ContentValues buddyValues = new ContentValues();
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE, accountType);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_NICK, buddyNick);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, groupId);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, statusIndex);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE, statusTitle);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE, statusMessage);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, 0);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime);
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, StringUtil.getAlphabetIndex(buddyNick));
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, buddyNick.toUpperCase());
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_LAST_SEEN, lastSeen);
        String avatarHash;
        QueryBuilder queryBuilder = new QueryBuilder();
        // Get buddy priority - from current group, then from non-recycle, then from recycle.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                .sortOrderRaw("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_GROUP + "=" + StringUtil.escapeSqlWithQuotes(groupName) + " THEN 2 ELSE 0 END)", "DESC").andOrder()
                .sortOrderRaw("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_GROUP_ID + "!=" + GlobalProvider.GROUP_ID_RECYCLE + " THEN 1 ELSE 0 END" + ")", "DESC")
                .limit(1);
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, queryBuilder);
            long buddyDbId = buddyCursor.getBuddyDbId();
            boolean buddyDialogFlag = buddyCursor.getBuddyDialog();
            avatarHash = buddyCursor.getBuddyAvatarHash();
            int buddyOperation = buddyCursor.getBuddyOperation();
            // Update dialog flag.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, buddyDialogFlag ? 1 : 0);
            // Checking for no buddy icon now, so, we must reset avatar hash.
            if (TextUtils.isEmpty(buddyIcon) && !TextUtils.isEmpty(avatarHash)) {
                buddyValues.putNull(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH);
            }
            // Checking for rename operation label.
            if (buddyOperation == GlobalProvider.ROSTER_BUDDY_OPERATION_RENAME) {
                if (TextUtils.equals(buddyCursor.getBuddyNick(), buddyNick)) {
                    // Nick is same. Remove rename label.
                    buddyOperation = GlobalProvider.ROSTER_BUDDY_OPERATION_NO;
                } else {
                    // Nick is not equals. This maybe roster before
                    // operation completed. Wait for same nick name.
                    buddyValues.remove(GlobalProvider.ROSTER_BUDDY_NICK);
                    buddyValues.remove(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX);
                    buddyValues.remove(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD);
                }
            }
            // Checking adding operation.
            if (buddyOperation == GlobalProvider.ROSTER_BUDDY_OPERATION_ADD) {
                // No more need in this flag. This buddy is permanent now.
                buddyOperation = GlobalProvider.ROSTER_BUDDY_OPERATION_NO;
            }
            // Update operation flag.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, buddyOperation);
            // Update this row.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
            databaseLayer.update(Settings.BUDDY_RESOLVER_URI, buddyValues, queryBuilder);
        } catch (BuddyNotFoundException ignored) {
            avatarHash = null;
            databaseLayer.insert(Settings.BUDDY_RESOLVER_URI, buddyValues);
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }

        if (!TextUtils.isEmpty(buddyIcon) && !TextUtils.equals(avatarHash, HttpUtil.getUrlHash(buddyIcon))) {
            // Avatar is ready.
            RequestHelper.requestBuddyAvatar(databaseLayer, accountDbId, buddyId, buddyIcon);
        }
    }

    public static void updateOrCreateGroup(ContentResolver contentResolver, int accountDbId, long updateTime,
                                           String groupName, int groupId) {
        updateOrCreateGroup(ContentResolverLayer.getInstance(contentResolver), accountDbId, updateTime, groupName, groupId);
    }

    public static void updateOrCreateGroup(SQLiteDatabase sqLiteDatabase, int accountDbId, long updateTime,
                                           String groupName, int groupId) {
        updateOrCreateGroup(SQLiteDatabaseLayer.getInstance(sqLiteDatabase), accountDbId, updateTime, groupName, groupId);
    }

    public static void updateOrCreateGroup(DatabaseLayer databaseLayer, int accountDbId, long updateTime,
                                           String groupName, int groupId) {
        ContentValues groupValues = new ContentValues();
        groupValues.put(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_NAME, groupName);
        groupValues.put(GlobalProvider.ROSTER_GROUP_ID, groupId);
        groupValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_DEFAULT);
        groupValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, updateTime);
        // Trying to update group.
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_NAME, groupName).and()
                .columnEquals(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
        int groupsModified = databaseLayer.update(Settings.GROUP_RESOLVER_URI, groupValues, queryBuilder);
        // Checking for there is no such group.
        if (groupsModified == 0) {
            databaseLayer.insert(Settings.GROUP_RESOLVER_URI, groupValues);
        }
    }

    private static void checkOrCreateRecycleGroup(ContentResolver contentResolver, Resources resources) {
        String recycleString = resources.getString(R.string.recycle);

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM)
                .and().columnEquals(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        Cursor recycleCursor = null;
        try {
            recycleCursor = queryBuilder.query(contentResolver, Settings.GROUP_RESOLVER_URI);
            if (!recycleCursor.moveToFirst()) {
                ContentValues recycleValues = new ContentValues();
                recycleValues.put(GlobalProvider.ROSTER_GROUP_NAME, recycleString);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, System.currentTimeMillis());
                contentResolver.insert(Settings.GROUP_RESOLVER_URI, recycleValues);
            }
        } finally {
            if (recycleCursor != null) {
                recycleCursor.close();
            }
        }
    }

    public static void moveBuddyIntoRecycle(ContentResolver contentResolver, Resources resources,
                                            int buddyDbId) {
        // To move buddy into recycle, we must have such recycle.
        checkOrCreateRecycleGroup(contentResolver, resources);
        // Now, we can move with pleasure.
        String recycleString = resources.getString(R.string.recycle);
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, recycleString);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
        modifyBuddy(contentResolver, buddyDbId, contentValues);
    }

    public static void removeOutdatedBuddies(ContentResolver contentResolver, int accountDbId, long updateTime) {
        removeOutdatedBuddies(ContentResolverLayer.getInstance(contentResolver), accountDbId, updateTime);
    }

    public static void removeOutdatedBuddies(SQLiteDatabase sqLiteDatabase, int accountDbId, long updateTime) {
        removeOutdatedBuddies(SQLiteDatabaseLayer.getInstance(sqLiteDatabase), accountDbId, updateTime);
    }

    public static void removeOutdatedBuddies(DatabaseLayer databaseLayer, int accountDbId, long updateTime) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Remove all deleted buddies.
        queryBuilder.columnNotEquals(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, updateTime)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_ADD)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        int removedBuddies = databaseLayer.delete(Settings.BUDDY_RESOLVER_URI, queryBuilder);
        Logger.log("outdated removed: " + removedBuddies);
    }

    public static void removeBuddy(ContentResolver contentResolver, int buddyDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
        queryBuilder.delete(contentResolver, Settings.BUDDY_RESOLVER_URI);
    }

    public static Collection<Integer> getBuddyDbIds(ContentResolver contentResolver, int accountDbId,
                                                    String buddyId, Map<String, Object> criteria)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        // Appending criteria values.
        for (String key : criteria.keySet()) {
            queryBuilder.and().columnEquals(key, criteria.get(key));
        }
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            List<Integer> buddyDbIds = new ArrayList<Integer>();
            do {
                int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
                buddyDbIds.add(buddyDbId);
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            return buddyDbIds;
        }
        // Closing cursor.
        cursor.close();
        throw new BuddyNotFoundException();
    }

    public static int getBuddyDbId(ContentResolver contentResolver, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry. Let's get first.
        if (cursor.moveToFirst()) {
            int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            return buddyDbId;
        }
        // Closing cursor.
        cursor.close();
        throw new BuddyNotFoundException();
    }

    public static int getBuddyDbId(ContentResolver contentResolver, int accountDbId, String groupName, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_GROUP, groupName)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            // Closing cursor.
            cursor.close();
            return buddyDbId;
        }
        // Closing cursor.
        cursor.close();
        throw new BuddyNotFoundException();
    }

    private static BuddyCursor getBuddyCursor(ContentResolver contentResolver, QueryBuilder queryBuilder)
            throws BuddyNotFoundException {
        return getBuddyCursor(ContentResolverLayer.getInstance(contentResolver), queryBuilder);
    }

    private static BuddyCursor getBuddyCursor(DatabaseLayer databaseLayer, QueryBuilder queryBuilder)
            throws BuddyNotFoundException {
        Cursor cursor = databaseLayer.query(Settings.BUDDY_RESOLVER_URI, queryBuilder);
        BuddyCursor buddyCursor = new BuddyCursor(cursor);
        if (buddyCursor.moveToFirst()) {
            return buddyCursor;
        } else {
            buddyCursor.close();
        }
        throw new BuddyNotFoundException();
    }

    public static BuddyCursor getRosterBuddyCursor(ContentResolver contentResolver, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified buddy. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE);
        return getBuddyCursor(contentResolver, queryBuilder);
    }

    public static BuddyCursor getBuddyCursor(ContentResolver contentResolver, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified buddy. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        return getBuddyCursor(contentResolver, queryBuilder);
    }

    public static BuddyCursor getBuddyCursor(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        return getBuddyCursor(contentResolver, new QueryBuilder().columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId));
    }

    public static int getBuddyAccountDbId(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = getBuddyCursor(contentResolver, buddyDbId);
        try {
            return buddyCursor.getBuddyAccountDbId();
        } finally {
            buddyCursor.close();
        }
    }

    public static String getBuddyNick(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = getBuddyCursor(contentResolver, buddyDbId);
        try {
            return buddyCursor.getBuddyNick();
        } finally {
            buddyCursor.close();
        }
    }

    public static String getBuddyDraft(ContentResolver contentResolver, int buddyDbId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = getBuddyCursor(contentResolver, buddyDbId);
        try {
            return buddyCursor.getBuddyDraft();
        } finally {
            buddyCursor.close();
        }
    }

    public static String getBuddyAvatarHash(ContentResolver contentResolver, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = getBuddyCursor(contentResolver, accountDbId, buddyId);
        try {
            return buddyCursor.getBuddyAvatarHash();
        } finally {
            buddyCursor.close();
        }
    }

    public static boolean checkDialog(ContentResolver contentResolver, int buddyDbId) {
        try {
            BuddyCursor buddyCursor = getBuddyCursor(contentResolver, buddyDbId);
            try {
                return buddyCursor.getBuddyDialog();
            } finally {
                buddyCursor.close();
            }
        } catch (BuddyNotFoundException ignored) {
            // No buddy - no dialog.
            return false;
        }
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

    public static String getAccountType(ContentResolver contentResolver, int accountDbId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain specified account. If exist.
        Cursor cursor = queryBuilder.query(contentResolver, Settings.ACCOUNT_RESOLVER_URI);
        // Checking for there is at least one account and switching to it.
        if (cursor.moveToFirst()) {
            String accountType = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE));
            // Closing cursor.
            cursor.close();
            return accountType;
        }
        throw new AccountNotFoundException();
    }

    public static void clearHistory(ContentResolver contentResolver, int buddyDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        queryBuilder.delete(contentResolver, Settings.HISTORY_RESOLVER_URI);
    }

    public static int getMoreActiveDialog(ContentResolver contentResolver)
            throws BuddyNotFoundException, MessageNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Query for opened dialogs.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_DIALOG, 1);
        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            int buddyDbIdColumn = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            // Creating query to history table, contains all messages from all opened dialogs.
            queryBuilder.recycle();
            do {
                int buddyDbId = cursor.getInt(buddyDbIdColumn);
                queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                if (!cursor.isLast()) {
                    queryBuilder.or();
                }
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            // Query for the most recent message.
            queryBuilder.descending(GlobalProvider.ROW_AUTO_ID);
            cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
            // Cursor may have more than only one entry. We need only first.
            if (cursor.moveToFirst()) {
                buddyDbIdColumn = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
                int moreActiveBuddyDbId = cursor.getInt(buddyDbIdColumn);
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

    public static void updateShownMessagesFlag(ContentResolver contentResolver) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and().startComplexExpression()
                .startComplexExpression()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 0)
                .finishComplexExpression()
                .or().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, -1)
                .finishComplexExpression();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateOnScreenMessages(ContentResolver contentResolver) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                .and()
                .startComplexExpression()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 1)
                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 0)
                .finishComplexExpression();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, -1);
        queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateBuddyOrAccountAvatar(AccountRoot accountRoot, String buddyId, String hash) {
        // Check for destination buddy is account.
        if (TextUtils.equals(buddyId, accountRoot.getUserId())) {
            accountRoot.setAvatarHash(hash);
            accountRoot.updateAccount();
        }
        try {
            // Attempt to update buddy avatar.
            QueryHelper.modifyBuddyAvatar(accountRoot.getContentResolver(),
                    accountRoot.getAccountDbId(), buddyId, hash);
        } catch (BuddyNotFoundException ignored) {
        }
    }

    public static String getBuddyOrAccountAvatarHash(AccountRoot accountRoot, String buddyId)
            throws AccountNotFoundException, BuddyNotFoundException {
        // Check for destination buddy is account.
        if (TextUtils.equals(buddyId, accountRoot.getUserId())) {
            return getAccountAvatarHash(accountRoot.getContentResolver(), accountRoot.getAccountDbId());
        }
        return getBuddyAvatarHash(accountRoot.getContentResolver(), accountRoot.getAccountDbId(), buddyId);
    }
}
