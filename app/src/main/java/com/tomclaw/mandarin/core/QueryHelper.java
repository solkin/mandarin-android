package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.MessageCursor;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.im.SentMessageData;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.StrictBuddy;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_BUDDY_ID;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_MESSAGE_COOKIE;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_MESSAGE_ID;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_MESSAGE_ID_START;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/9/13
 * Time: 2:13 PM
 */
@SuppressWarnings("WeakerAccess")
public class QueryHelper {

    private QueryHelper() {
    }

    public static List<AccountRoot> getAccounts(Context context, DatabaseLayer databaseLayer,
                                                List<AccountRoot> accountRootList) {
        // Clearing input list.
        accountRootList.clear();
        // Obtain specified account. If exist.
        Cursor cursor = databaseLayer.query(Settings.ACCOUNT_RESOLVER_URI, new QueryBuilder());
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

    public static AccountRoot getAccount(Context context, DatabaseLayer databaseLayer,
                                         int accountDbId) {
        AccountRoot accountRoot = null;
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
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

    public static int getAccountDbId(DatabaseLayer databaseLayer, String accountType, String userId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ACCOUNT_TYPE, accountType)
                .and().columnEquals(GlobalProvider.ACCOUNT_USER_ID, userId);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
            // Cursor may have no more than only one entry. But lets check.
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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

    public static void updateAccount(Context context, DatabaseLayer databaseLayer,
                                     AccountRoot accountRoot)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType()).and()
                .columnEquals(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
            // Cursor may have only one entry.
            if (cursor.moveToFirst()) {
                long accountDbId = cursor.getLong(cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID));
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
                queryBuilder.update(databaseLayer, contentValues, Settings.ACCOUNT_RESOLVER_URI);
                if (accountRoot.isOffline()) {
                    // Update status for account buddies to unknown.
                    contentValues = new ContentValues();
                    contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
                    // Update query.
                    queryBuilder.recycle();
                    queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                    queryBuilder.update(databaseLayer, contentValues, Settings.BUDDY_RESOLVER_URI);
                }
                return;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        throw new AccountNotFoundException();
    }

    public static boolean isAccountActive(DatabaseLayer databaseLayer, int accountDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId)
                .and().columnNotEquals(GlobalProvider.ACCOUNT_STATUS, StatusUtil.STATUS_OFFLINE)
                .and().columnEquals(GlobalProvider.ACCOUNT_CONNECTING, 0);
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
        // Checking for condition is satisfied.
        boolean accountActive = cursor.moveToFirst();
        cursor.close();
        // Closing cursor.
        return accountActive;
    }

    public static Cursor getActiveAccounts(DatabaseLayer databaseLayer) {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnNotEquals(GlobalProvider.ACCOUNT_STATUS, StatusUtil.STATUS_OFFLINE)
                .and().columnEquals(GlobalProvider.ACCOUNT_CONNECTING, 0);
        // Not so good decision to return simple cursor!
        return queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
    }

    public static int getAccountsCount(DatabaseLayer databaseLayer) {
        QueryBuilder queryBuilder = new QueryBuilder();
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
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
     * @param context       - context for account root
     * @param databaseLayer - database layer to be used
     * @param accountRoot   - account root to be inserted into database
     * @return account db id.
     * @throws AccountNotFoundException, AccountAlreadyExistsException
     */
    public static int insertAccount(Context context, DatabaseLayer databaseLayer, AccountRoot accountRoot)
            throws AccountNotFoundException {
        try {
            int accountDbId = getAccountDbId(databaseLayer,
                    accountRoot.getAccountType(), accountRoot.getUserId());
            updateAccount(context, databaseLayer, accountRoot);
            return accountDbId;
        } catch (AccountNotFoundException ex) {
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
            databaseLayer.insert(Settings.ACCOUNT_RESOLVER_URI, contentValues);
            // Setting up account db id.
            accountRoot.setAccountDbId(getAccountDbId(databaseLayer,
                    accountRoot.getAccountType(), accountRoot.getUserId()));
            accountRoot.setContext(context);
            return accountRoot.getAccountDbId();
        }
    }

    public static boolean removeAccount(DatabaseLayer databaseLayer, int accountDbId) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        // Obtain account db id.
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
        // Cursor may have no more than only one entry. But lets check.
        if (cursor.moveToFirst()) {
            do {
                QueryBuilder removeBuilder = new QueryBuilder();
                removeBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
                // Removing roster groups.
                removeBuilder.delete(databaseLayer, Settings.GROUP_RESOLVER_URI);
                // Removing roster buddies.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(databaseLayer, Settings.BUDDY_RESOLVER_URI);
                // Removing all the history.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(databaseLayer, Settings.HISTORY_RESOLVER_URI);
                // Removing all pending requests.
                removeBuilder.recycle();
                removeBuilder.columnEquals(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
                removeBuilder.delete(databaseLayer, Settings.REQUEST_RESOLVER_URI);
            } while (cursor.moveToNext());
        }
        // Closing cursor.
        cursor.close();
        // And remove account.
        return queryBuilder.delete(databaseLayer, Settings.ACCOUNT_RESOLVER_URI) != 0;
    }

    public static void updateAccountAvatar(DatabaseLayer databaseLayer, int accountDbId, String avatarHash) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ACCOUNT_AVATAR_HASH, avatarHash);
        modifyAccount(databaseLayer, accountDbId, contentValues);
    }

    public static String getAccountAvatarHash(DatabaseLayer databaseLayer, int accountDbId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified accounts. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
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

    private static void modifyAccount(DatabaseLayer databaseLayer, int accountDbId, ContentValues contentValues) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        queryBuilder.update(databaseLayer, contentValues, Settings.ACCOUNT_RESOLVER_URI);
    }

    public static void modifyBuddyDraft(DatabaseLayer databaseLayer, Buddy buddy, String buddyDraft) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DRAFT, buddyDraft);
        modifyBuddies(databaseLayer, Collections.singleton(buddy), contentValues);
    }

    public static void modifyDialog(DatabaseLayer databaseLayer, Buddy buddy, boolean isOpened) {
        modifyDialogs(databaseLayer, Collections.singleton(buddy), isOpened);
    }

    public static void modifyDialog(DatabaseLayer databaseLayer, Buddy buddy,
                                    boolean isOpened, long lastMessageTime) {
        modifyDialogs(databaseLayer, Collections.singleton(buddy), isOpened, lastMessageTime);
    }

    public static void modifyDialogs(DatabaseLayer databaseLayer, Collection<Buddy> buddies, boolean isOpened) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        modifyBuddies(databaseLayer, buddies, contentValues);
    }

    public static void modifyDialogs(DatabaseLayer databaseLayer, Collection<Buddy> buddies,
                                     boolean isOpened, long lastMessageTime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isOpened ? 1 : 0);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME, lastMessageTime);
        modifyBuddies(databaseLayer, buddies, contentValues);
    }

    public static void modifyOperation(DatabaseLayer databaseLayer, Buddy buddy, int operation) {
        modifyOperation(databaseLayer, Collections.singleton(buddy), operation);
    }

    public static void modifyOperation(DatabaseLayer databaseLayer, Collection<Buddy> buddy, int operation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, operation);
        modifyBuddies(databaseLayer, buddy, contentValues);
    }

    public static void modifyBuddyNick(DatabaseLayer databaseLayer, Buddy buddy,
                                       String buddyNick, boolean isStartOperation) {
        modifyBuddyNick(databaseLayer, Collections.singleton(buddy), buddyNick, isStartOperation);
    }

    public static void modifyBuddyNick(DatabaseLayer databaseLayer, Collection<Buddy> buddies,
                                       String buddyNick, boolean isStartOperation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_NICK, buddyNick);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, Strings.getAlphabetIndex(buddyNick));
        contentValues.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, buddyNick.toUpperCase());
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, isStartOperation ?
                GlobalProvider.ROSTER_BUDDY_OPERATION_RENAME : GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
        modifyBuddies(databaseLayer, buddies, contentValues);
    }

//    public static void insertMessage(ContentResolver contentResolver, int buddyDbId,
//                                     int messageType, String cookie, String messageText)
//            throws BuddyNotFoundException {
//        insertTextMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
//                messageType, cookie, 0, messageText);
//    }

//    public static void insertOutgoingFileMessage(ContentResolver contentResolver, int buddyDbId, String cookie,
//                                                 Uri uri, String name, int contentType, long contentSize,
//                                                 String previewHash, String contentTag)
//            throws BuddyNotFoundException {
//        insertFileMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
//                GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie, 0, "", contentType,
//                contentSize, GlobalProvider.HISTORY_CONTENT_STATE_WAITING, uri.toString(),
//                name, previewHash, contentTag);
//    }
//
//    public static void insertIncomingFileMessage(ContentResolver contentResolver, int buddyDbId, String cookie,
//                                                 long time, String originalMessage, Uri uri, String name, int contentType,
//                                                 long contentSize, String previewHash, String contentTag) throws BuddyNotFoundException {
//        insertFileMessage(contentResolver, getBuddyAccountDbId(contentResolver, buddyDbId), buddyDbId,
//                GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, cookie, time, originalMessage,
//                contentType, contentSize, GlobalProvider.HISTORY_CONTENT_STATE_WAITING,
//                uri.toString(), name, previewHash, contentTag);
//    }

//    public static void insertFileMessage(ContentResolver contentResolver, int accountDbId,
//                                         int buddyDbId, int messageType, String cookie,
//                                         long messageTime, String messageText, int contentType,
//                                         long contentSize, int contentState, String contentUri,
//                                         String contentName, String previewHash, String contentTag) {
//        // Checking for time specified.
//        if (messageTime == 0) {
//            messageTime = System.currentTimeMillis();
//        }
//        // Update last message time and make dialog opened.
//        modifyDialog(contentResolver, buddyDbId, true, messageTime);
//        // No matching request message. Insert new message.
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
//        contentValues.put(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
//        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
//        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, messageTime);
//        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, messageText);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, contentType);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_SIZE, contentSize);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, contentState);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_URI, contentUri);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_NAME, contentName);
//        contentValues.put(GlobalProvider.HISTORY_PREVIEW_HASH, previewHash);
//        contentValues.put(GlobalProvider.HISTORY_CONTENT_TAG, contentTag);
//        // Try to modify message or create it.
//        if (modifyFile(contentResolver, contentValues, messageType, cookie) == 0) {
//            contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
//            contentResolver.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
//        }
//    }

    public static QueryBuilder messageQueryBuilder(MessageData message) {
        return new QueryBuilder()
                .columnEquals(HISTORY_BUDDY_ACCOUNT_DB_ID, message.getBuddyAccountDbId()).and()
                .columnEquals(HISTORY_BUDDY_ID, message.getBuddyId()).and()
                .columnEquals(HISTORY_MESSAGE_ID, message.getMessageId());
    }

    public static QueryBuilder sentMessageQueryBuilder(SentMessageData data) {
        return new QueryBuilder()
                .columnEquals(HISTORY_MESSAGE_COOKIE, data.getCookie());
    }

    public static void insertOrUpdateMessage(DatabaseLayer databaseLayer, MessageData message) {
        boolean isUpdate = isMessageExist(databaseLayer, message);
        Logger.log("insertTextMessage: " + message.getMessageId() + " will be "
                + (isUpdate ? "updated" : "inserted"));
        if (isUpdate) {
            updateMessage(databaseLayer, message);
        } else {
            insertMessage(databaseLayer, message);
        }
    }

    public static boolean isMessageExist(DatabaseLayer databaseLayer, MessageData message) {
        Cursor cursor = null;
        try {
            cursor = messageQueryBuilder(message)
                    .query(databaseLayer, Settings.HISTORY_RESOLVER_URI);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // TODO: add content support
    public static void insertMessage(DatabaseLayer databaseLayer, MessageData message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, message.getBuddyAccountDbId());
        contentValues.put(GlobalProvider.HISTORY_BUDDY_ID, message.getBuddyId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, message.getMessagePrevId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_ID, message.getMessageId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TYPE, message.getMessageType());
        if (message.getCookie() == null) {
            contentValues.putNull(GlobalProvider.HISTORY_MESSAGE_COOKIE);
        } else {
            contentValues.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, message.getCookie());
        }
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, message.getMessageTime());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, message.getMessageText());
        contentValues.put(GlobalProvider.HISTORY_CONTENT_TYPE, GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);
        databaseLayer.insert(Settings.HISTORY_RESOLVER_URI, contentValues);
    }

    // TODO: add content support
    private static void updateMessage(DatabaseLayer databaseLayer, MessageData message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, message.getMessagePrevId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, message.getMessageTime());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, message.getMessageText());
        messageQueryBuilder(message)
                .update(databaseLayer, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateSentMessage(DatabaseLayer databaseLayer, SentMessageData data) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_ID, data.getMessageId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, data.getMessagePrevId());
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TIME, data.getMessageTime());

        sentMessageQueryBuilder(data)
                .update(databaseLayer, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    private static int modifyFile(DatabaseLayer databaseLayer, ContentValues contentValues, int messageType, String cookie) {
        // Plain message modify by cookies.
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType)
                .and().like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        return queryBuilder.update(databaseLayer, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static void updateFileState(DatabaseLayer databaseLayer, int state, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        int modified = modifyFile(databaseLayer, contentValues, messageType, cookie);
        Logger.log("modified: " + modified);
    }

    public static void updateFileSize(DatabaseLayer databaseLayer, long size, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_SIZE, size);
        modifyFile(databaseLayer, contentValues, messageType, cookie);
    }

    public static void updateFileProgress(DatabaseLayer databaseLayer, int progress, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_PROGRESS, progress);
        modifyFile(databaseLayer, contentValues, messageType, cookie);
    }

    public static void updateFileStateAndText(DatabaseLayer databaseLayer, int state,
                                              String text, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_TEXT, text);
        modifyFile(databaseLayer, contentValues, messageType, cookie);
    }

    public static void revertFileToMessage(DatabaseLayer databaseLayer, int messageType,
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
        modifyFile(databaseLayer, contentValues, messageType, cookie);
    }

    public static void updateFileStateAndHash(DatabaseLayer databaseLayer, int state,
                                              String hash, int messageType, String cookie) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_CONTENT_STATE, state);
        contentValues.put(GlobalProvider.HISTORY_PREVIEW_HASH, hash);
        modifyFile(databaseLayer, contentValues, messageType, cookie);
    }

    public static int getFileState(DatabaseLayer databaseLayer, int messageType, String cookie)
            throws MessageNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType)
                .and().like(GlobalProvider.HISTORY_MESSAGE_COOKIE, cookie);
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.HISTORY_RESOLVER_URI);
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

    public static void removeMessages(DatabaseLayer databaseLayer, Collection<Long> messageIds) {
        messagesByIds(messageIds).delete(databaseLayer, Settings.HISTORY_RESOLVER_URI);
    }

    public static void removeMessagesUpTo(DatabaseLayer databaseLayer, Buddy buddy, long messageId) {
        int accountDbId = buddy.getAccountDbId();
        String buddyId = buddy.getBuddyId();
        new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId).and()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ID, buddyId).and()
                .less(GlobalProvider.HISTORY_MESSAGE_ID, messageId)
                .delete(databaseLayer, Settings.HISTORY_RESOLVER_URI);
    }

    public static void markMessageRequested(DatabaseLayer databaseLayer, Buddy buddy, long messageId) {
        modifyMessagePrevId(databaseLayer, buddy, messageId, GlobalProvider.HISTORY_MESSAGE_ID_REQUESTED);
    }

    public static void modifyMessagePrevId(DatabaseLayer databaseLayer, Buddy buddy,
                                           long messageId, long messagePrevId) {
        int accountDbId = buddy.getAccountDbId();
        String buddyId = buddy.getBuddyId();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.HISTORY_MESSAGE_PREV_ID, messagePrevId);
        new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId).and()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ID, buddyId).and()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_ID, messageId)
                .update(databaseLayer, contentValues, Settings.HISTORY_RESOLVER_URI);
    }

    public static long getLastIncomingMessageId(DatabaseLayer databaseLayer, Buddy buddy) {
        try {
            MessageData messageData = getLastIncomingMessage(databaseLayer, buddy);
            return messageData.getMessageId();
        } catch (MessageNotFoundException e) {
            return HISTORY_MESSAGE_ID_START;
        }
    }

    public static MessageData getLastIncomingMessage(DatabaseLayer databaseLayer, Buddy buddy)
            throws MessageNotFoundException {
        int accountDbId = buddy.getAccountDbId();
        String buddyId = buddy.getBuddyId();
        MessageCursor cursor = null;
        try {
            cursor = getMessageCursor(databaseLayer, new QueryBuilder()
                    .columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId).and()
                    .columnEquals(GlobalProvider.HISTORY_BUDDY_ID, buddyId).and()
                    .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING)
                    .descending(GlobalProvider.HISTORY_MESSAGE_ID)
                    .limit(1));
            return cursor.toMessageData();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean isIncomingMessagesPresent(DatabaseLayer databaseLayer, Collection<Long> messageIds) {
        QueryBuilder queryBuilder = messagesByIds(messageIds).and()
                .columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING);
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.HISTORY_RESOLVER_URI);
        return cursor.getCount() > 0;
    }

    private static MessageCursor getMessageCursor(DatabaseLayer databaseLayer, QueryBuilder queryBuilder)
            throws MessageNotFoundException {
        Cursor cursor = databaseLayer.query(Settings.HISTORY_RESOLVER_URI, queryBuilder);
        MessageCursor messageCursor = new MessageCursor(cursor);
        if (messageCursor.moveToFirst()) {
            return messageCursor;
        } else {
            messageCursor.close();
        }
        throw new MessageNotFoundException();
    }

//    /**
//     * This method helps to get formatted messages from history by ids.
//     * But this method is really, really strange!
//     * Should be rewritten. Sometime.
//     *
//     * @param contentResolver - plain content resolver.
//     * @param timeHelper      - time helper to format messages time
//     * @param messageIds      - messages to be queried.
//     * @return formatted messages.
//     */
//    public static String getMessagesTexts(ContentResolver contentResolver, TimeHelper timeHelper, Collection<Long> messageIds) {
//        StringBuilder messageBuilder = new StringBuilder();
//        QueryBuilder queryBuilder = messagesByIds(messageIds);
//        // Get specified messages.
//        Cursor cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
//        try {
//            if (cursor != null && cursor.moveToFirst()) {
//                do {
//                    int messageType = cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE));
//                    String messageText = cursor.getString(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT));
//                    long messageTime = cursor.getLong(cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME));
//                    String messageTimeText = timeHelper.getFormattedTime(messageTime);
//                    String messageDateText = timeHelper.getFormattedDate(messageTime);
//                    int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID));
//                    int buddyDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID));
//                    String buddyNick = "Unknown";
//                    try {
//                        // Select message type.
//                        switch (messageType) {
//                            case GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING: {
//                                buddyNick = QueryHelper.getBuddyNick(contentResolver, buddyDbId);
//                                break;
//                            }
//                            case GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING: {
//                                buddyNick = QueryHelper.getAccountName(contentResolver, accountDbId);
//                                break;
//                            }
//                        }
//                    } catch (BuddyNotFoundException ignored) {
//                    } catch (AccountNotFoundException ignored) {
//                    }
//                    // Building message copy.
//                    messageBuilder.append('[').append(buddyNick).append(']').append('\n');
//                    messageBuilder.append(messageDateText).append(" - ").append(messageTimeText).append('\n');
//                    messageBuilder.append(messageText);
//                    messageBuilder.append('\n').append('\n');
//                } while (cursor.moveToNext());
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return messageBuilder.toString();
//    }

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

    public static boolean checkBuddy(DatabaseLayer databaseLayer, int accountDbId, String buddyId) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId).and()
                .columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = null;
        try {
            cursor = databaseLayer.query(Settings.BUDDY_RESOLVER_URI, queryBuilder);
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static void modifyBuddy(DatabaseLayer databaseLayer, Buddy buddy, ContentValues contentValues) {
        modifyBuddies(databaseLayer, Collections.singleton(buddy), contentValues);
    }

    private static void modifyBuddies(DatabaseLayer databaseLayer, Collection<Buddy> buddies, ContentValues contentValues) {
        QueryBuilder queryBuilder = buddiesQueryBuilder(buddies);
        queryBuilder.update(databaseLayer, contentValues, Settings.BUDDY_RESOLVER_URI);
    }

    private static void modifyStrictBuddy(DatabaseLayer databaseLayer, StrictBuddy buddy, ContentValues contentValues) {
        modifyStrictBuddies(databaseLayer, Collections.singleton(buddy), contentValues);
    }

    private static void modifyStrictBuddies(DatabaseLayer databaseLayer, Collection<StrictBuddy> buddies, ContentValues contentValues) {
        QueryBuilder queryBuilder = strictBuddiesQueryBuilder(buddies);
        queryBuilder.update(databaseLayer, contentValues, Settings.BUDDY_RESOLVER_URI);
    }

    private static QueryBuilder buddiesQueryBuilder(Collection<Buddy> buddies) {
        QueryBuilder queryBuilder = new QueryBuilder();
        boolean isFirst = true;
        for (Buddy buddy : buddies) {
            if (isFirst) {
                isFirst = false;
            } else {
                queryBuilder.or();
            }
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            queryBuilder
                    .startComplexExpression()
                    .columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                    .and()
                    .columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                    .finishComplexExpression();
        }
        return queryBuilder;
    }

    private static QueryBuilder strictBuddiesQueryBuilder(Collection<StrictBuddy> buddies) {
        QueryBuilder queryBuilder = new QueryBuilder();
        boolean isFirst = true;
        for (StrictBuddy buddy : buddies) {
            if (isFirst) {
                isFirst = false;
            } else {
                queryBuilder.or();
            }
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            String groupName = buddy.getGroupName();
            queryBuilder
                    .startComplexExpression()
                    .columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                    .and()
                    .columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                    .and()
                    .columnEquals(GlobalProvider.ROSTER_BUDDY_GROUP, groupName)
                    .finishComplexExpression();
        }
        return queryBuilder;
    }

    public static void modifyBuddyAvatar(DatabaseLayer databaseLayer, int accountDbId, String buddyId,
                                         String avatarHash) throws BuddyNotFoundException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH, avatarHash);
        Buddy buddy = new Buddy(accountDbId, buddyId);
        modifyBuddy(databaseLayer, buddy, contentValues);
    }

    public static void modifyBuddyStatus(DatabaseLayer databaseLayer, int accountDbId, String buddyId,
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
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.BUDDY_RESOLVER_URI);
        // Cursor may have more than only one entry.
        if (cursor.moveToFirst()) {
            // Cycling all the identical buddies in different groups.
            do {
                avatarHash = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH));
                // Checking for no buddy icon now, so, we must reset avatar hash.
                if (TextUtils.isEmpty(buddyIcon) && !TextUtils.isEmpty(avatarHash)) {
                    contentValues.putNull(GlobalProvider.ROSTER_BUDDY_AVATAR_HASH);
                }
                Buddy buddy = new Buddy(accountDbId, buddyId);
                modifyBuddy(databaseLayer, buddy, contentValues);
            } while (cursor.moveToNext());
            // Closing cursor.
            cursor.close();
            // There are may be a lot of buddies in lots of groups, but this is the same buddy with the save avatar.
            if (!TextUtils.isEmpty(buddyIcon) && !TextUtils.equals(avatarHash, HttpUtil.getUrlHash(buddyIcon))) {
                // Avatar is ready.
                RequestHelper.requestBuddyAvatar(databaseLayer, accountDbId, buddyId, buddyIcon);
            }
        } else {
            // Closing cursor.
            cursor.close();
            throw new BuddyNotFoundException();
        }
    }

    public static void modifyDialogState(DatabaseLayer databaseLayer, Buddy buddy, long unreadCnt,
                                         @Nullable Long lastMessageTime, long lastMsgId,
                                         long yoursLastRead, long theirsLastDelivered,
                                         long theirsLastRead, String patchVersion) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT, unreadCnt);
        if (lastMessageTime != null) {
            contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME, lastMessageTime);
        }
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_ID, lastMsgId);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_YOURS_LAST_READ, yoursLastRead);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_THEIRS_LAST_DELIVERED, theirsLastDelivered);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_THEIRS_LAST_READ, theirsLastRead);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_PATCH_VERSION, patchVersion);
        modifyBuddy(databaseLayer, buddy, contentValues);
    }

    public static void modifyBuddyYoursReadState(DatabaseLayer databaseLayer, Buddy buddy,
                                                 long unreadCnt, long yoursLastRead) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT, unreadCnt);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_YOURS_LAST_READ, yoursLastRead);
        modifyBuddy(databaseLayer, buddy, contentValues);
    }

    public static void modifyBuddyNotifiedMessageId(DatabaseLayer databaseLayer, Buddy buddy,
                                                    long notifiedMsgId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_NOTIFIED_MSG_ID, notifiedMsgId);
        modifyBuddy(databaseLayer, buddy, contentValues);
    }

    public static void modifyBuddyTyping(DatabaseLayer databaseLayer, int accountDbId, String buddyId,
                                         boolean isTyping) throws BuddyNotFoundException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_LAST_TYPING, isTyping ? System.currentTimeMillis() : 0);
        Buddy buddy = new Buddy(accountDbId, buddyId);
        modifyBuddy(databaseLayer, buddy, contentValues);
    }

    public static void replaceOrCreateBuddy(DatabaseLayer databaseLayer, int accountDbId, String accountType,
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
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, Strings.getAlphabetIndex(buddyNick));
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
            buddyCursor = getBuddyCursor(databaseLayer, queryBuilder);
            long buddyDbId = buddyCursor.getDbId();
            boolean buddyDialogFlag = buddyCursor.getDialog();
            // Update dialog flag.
            buddyValues.put(GlobalProvider.ROSTER_BUDDY_DIALOG, buddyDialogFlag ? 1 : 0);
            // Update this row.
            queryBuilder.recycle();
            queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
            queryBuilder.update(databaseLayer, buddyValues, Settings.BUDDY_RESOLVER_URI);
        } catch (BuddyNotFoundException ignored) {
            databaseLayer.insert(Settings.BUDDY_RESOLVER_URI, buddyValues);
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
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
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, Strings.getAlphabetIndex(buddyNick));
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, buddyNick.toUpperCase());
        buddyValues.put(GlobalProvider.ROSTER_BUDDY_LAST_SEEN, lastSeen);
        String avatarHash;
        QueryBuilder queryBuilder = new QueryBuilder();
        // Get buddy priority - from current group, then from non-recycle, then from recycle.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                .sortOrderRaw("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_GROUP + "=" + Strings.escapeSqlWithQuotes(groupName) + " THEN 2 ELSE 0 END)", "DESC").andOrder()
                .sortOrderRaw("(CASE WHEN " + GlobalProvider.ROSTER_BUDDY_GROUP_ID + "!=" + GlobalProvider.GROUP_ID_RECYCLE + " THEN 1 ELSE 0 END" + ")", "DESC")
                .limit(1);
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, queryBuilder);
            long buddyDbId = buddyCursor.getDbId();
            boolean buddyDialogFlag = buddyCursor.getDialog();
            avatarHash = buddyCursor.getAvatarHash();
            int buddyOperation = buddyCursor.getOperation();
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

    private static void checkOrCreateRecycleGroup(DatabaseLayer databaseLayer, Resources resources) {
        String recycleString = resources.getString(R.string.recycle);

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM)
                .and().columnEquals(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        Cursor recycleCursor = null;
        try {
            recycleCursor = queryBuilder.query(databaseLayer, Settings.GROUP_RESOLVER_URI);
            if (!recycleCursor.moveToFirst()) {
                ContentValues recycleValues = new ContentValues();
                recycleValues.put(GlobalProvider.ROSTER_GROUP_NAME, recycleString);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_SYSTEM);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
                recycleValues.put(GlobalProvider.ROSTER_GROUP_UPDATE_TIME, System.currentTimeMillis());
                databaseLayer.insert(Settings.GROUP_RESOLVER_URI, recycleValues);
            }
        } finally {
            if (recycleCursor != null) {
                recycleCursor.close();
            }
        }
    }

    public static void moveBuddyIntoRecycle(DatabaseLayer databaseLayer, Resources resources,
                                            StrictBuddy buddy) {
        // To move buddy into recycle, we must have such recycle.
        checkOrCreateRecycleGroup(databaseLayer, resources);
        // Now, we can move with pleasure.
        String recycleString = resources.getString(R.string.recycle);
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.ROSTER_BUDDY_GROUP, recycleString);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_STATUS, StatusUtil.STATUS_OFFLINE);
        contentValues.put(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
        modifyStrictBuddy(databaseLayer, buddy, contentValues);
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

    public static void removeBuddy(DatabaseLayer databaseLayer, StrictBuddy buddy) {
        QueryBuilder queryBuilder = strictBuddiesQueryBuilder(Collections.singletonList(buddy));
        queryBuilder.delete(databaseLayer, Settings.BUDDY_RESOLVER_URI);
    }

    public static Collection<Buddy> getBuddiesWithUnread(DatabaseLayer databaseLayer)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder()
                .columnNotEquals(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT, 0);
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = new BuddyCursor(queryBuilder.query(databaseLayer, Settings.BUDDY_RESOLVER_URI));
            if (buddyCursor.moveToFirst()) {
                Set<Buddy> buddies = new HashSet<>();
                do {
                    Buddy buddy = buddyCursor.toBuddy();
                    buddies.add(buddy);
                } while (buddyCursor.moveToNext());
                return buddies;
            }
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
        return Collections.emptySet();
    }

    public static Collection<Buddy> getBuddies(DatabaseLayer databaseLayer, int accountDbId,
                                               String buddyId, Map<String, Object> criteria)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId).and()
                .columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        // Appending criteria values.
        for (String key : criteria.keySet()) {
            queryBuilder.and().columnEquals(key, criteria.get(key));
        }
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = new BuddyCursor(queryBuilder.query(databaseLayer, Settings.BUDDY_RESOLVER_URI));
            if (buddyCursor.moveToFirst()) {
                Set<Buddy> buddies = new HashSet<>();
                do {
                    Buddy buddy = buddyCursor.toBuddy();
                    buddies.add(buddy);
                } while (buddyCursor.moveToNext());
                return new ArrayList<>(buddies);
            }
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
        throw new BuddyNotFoundException();
    }

    public static int getBuddyDbId(DatabaseLayer databaseLayer, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain account db id.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
        Cursor cursor = queryBuilder.query(databaseLayer, Settings.BUDDY_RESOLVER_URI);
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

    public static BuddyCursor getRosterBuddyCursor(DatabaseLayer databaseLayer, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        // Obtain specified buddy. If exist.
        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_GROUP_ID, GlobalProvider.GROUP_ID_RECYCLE)
                .and().columnNotEquals(GlobalProvider.ROSTER_BUDDY_OPERATION, GlobalProvider.ROSTER_BUDDY_OPERATION_REMOVE);
        return getBuddyCursor(databaseLayer, queryBuilder);
    }

    public static BuddyCursor getBuddyCursor(DatabaseLayer databaseLayer, Buddy buddy)
            throws BuddyNotFoundException {
        return getBuddyCursor(databaseLayer, buddy.getAccountDbId(), buddy.getBuddyId());
    }

    public static BuddyCursor getBuddyCursor(DatabaseLayer databaseLayer, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        return getBuddyCursor(databaseLayer, new QueryBuilder()
                .columnEquals(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId)
                .and().columnEquals(GlobalProvider.ROSTER_BUDDY_ID, buddyId));
    }

    public static BuddyCursor getBuddyCursor(DatabaseLayer databaseLayer, int buddyDbId)
            throws BuddyNotFoundException {
        return getBuddyCursor(databaseLayer, new QueryBuilder().columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId));
    }

    public static String getBuddyPatchVersion(DatabaseLayer databaseLayer, Buddy buddy) {
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, buddy);
            return buddyCursor.getPatchVersion();
        } catch (BuddyNotFoundException ignored) {
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
        return null;
    }

    public static long getBuddyYoursLastRead(DatabaseLayer databaseLayer, Buddy buddy) {
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, buddy);
            return buddyCursor.getYoursLastRead();
        } catch (BuddyNotFoundException ignored) {
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
        return 0;
    }

    public static String getBuddyId(DatabaseLayer databaseLayer, int buddyDbId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = getBuddyCursor(databaseLayer, buddyDbId);
        try {
            return buddyCursor.getBuddyId();
        } finally {
            buddyCursor.close();
        }
    }

    @Deprecated
    public static String getBuddyNick(DatabaseLayer databaseLayer, int buddyDbId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, buddyDbId);
            return buddyCursor.getBuddyNick();
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
    }

    @Deprecated
    public static String getBuddyDraft(DatabaseLayer databaseLayer, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, accountDbId, buddyId);
            return buddyCursor.getDraft();
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
    }

    @Deprecated
    public static String getBuddyAvatarHash(DatabaseLayer databaseLayer, int accountDbId, String buddyId)
            throws BuddyNotFoundException {
        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = getBuddyCursor(databaseLayer, accountDbId, buddyId);
            return buddyCursor.getAvatarHash();
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
    }

    public static String getAccountName(DatabaseLayer databaseLayer, int accountDbId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
            if (cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex(GlobalProvider.ACCOUNT_NAME);
                return cursor.getString(nameColumnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        throw new AccountNotFoundException();
    }

    public static String getAccountType(DatabaseLayer databaseLayer, int accountDbId)
            throws AccountNotFoundException {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.columnEquals(GlobalProvider.ROW_AUTO_ID, accountDbId);
        Cursor cursor = null;
        try {
            cursor = queryBuilder.query(databaseLayer, Settings.ACCOUNT_RESOLVER_URI);
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_TYPE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        throw new AccountNotFoundException();
    }

    public static void clearHistory(DatabaseLayer databaseLayer, Buddy buddy) {
        QueryBuilder queryBuilder = buddiesQueryBuilder(Collections.singletonList(buddy));
        queryBuilder.delete(databaseLayer, Settings.HISTORY_RESOLVER_URI);
    }

//    public static int getMoreActiveDialog(ContentResolver contentResolver)
//            throws BuddyNotFoundException, MessageNotFoundException {
//        QueryBuilder queryBuilder = new QueryBuilder();
//        // Query for opened dialogs.
//        queryBuilder.columnEquals(GlobalProvider.ROSTER_BUDDY_DIALOG, 1);
//        Cursor cursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
//        // Cursor may have more than only one entry.
//        if (cursor.moveToFirst()) {
//            int buddyDbIdColumn = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
//            // Creating query to history table, contains all messages from all opened dialogs.
//            queryBuilder.recycle();
//            do {
//                int buddyDbId = cursor.getInt(buddyDbIdColumn);
//                queryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
//                if (!cursor.isLast()) {
//                    queryBuilder.or();
//                }
//            } while (cursor.moveToNext());
//            // Closing cursor.
//            cursor.close();
//            // Query for the most recent message.
//            queryBuilder.descending(GlobalProvider.ROW_AUTO_ID);
//            cursor = queryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
//            // Cursor may have more than only one entry. We need only first.
//            if (cursor.moveToFirst()) {
//                buddyDbIdColumn = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
//                int moreActiveBuddyDbId = cursor.getInt(buddyDbIdColumn);
//                // Closing cursor.
//                cursor.close();
//                return moreActiveBuddyDbId;
//            } else {
//                // Closing cursor.
//                cursor.close();
//                // Really no messages.
//                throw new MessageNotFoundException();
//            }
//        } else {
//            // Closing cursor.
//            cursor.close();
//            // No opened dialogs.
//            throw new BuddyNotFoundException();
//        }
//    }

    @Deprecated
    public static void updateBuddyOrAccountAvatar(DatabaseLayer databaseLayer,
                                                  AccountRoot accountRoot,
                                                  String buddyId,
                                                  String hash) {
        // Check for destination buddy is account.
        if (TextUtils.equals(buddyId, accountRoot.getUserId())) {
            accountRoot.setAvatarHash(hash);
            accountRoot.updateAccount();
        }
        try {
            // Attempt to update buddy avatar.
            QueryHelper.modifyBuddyAvatar(databaseLayer,
                    accountRoot.getAccountDbId(), buddyId, hash);
        } catch (BuddyNotFoundException ignored) {
        }
    }

    @Deprecated
    public static String getBuddyOrAccountAvatarHash(DatabaseLayer databaseLayer,
                                                     AccountRoot accountRoot, String buddyId)
            throws AccountNotFoundException, BuddyNotFoundException {
        // Check for destination buddy is account.
        if (TextUtils.equals(buddyId, accountRoot.getUserId())) {
            return getAccountAvatarHash(databaseLayer, accountRoot.getAccountDbId());
        }
        return getBuddyAvatarHash(databaseLayer, accountRoot.getAccountDbId(), buddyId);
    }
}
