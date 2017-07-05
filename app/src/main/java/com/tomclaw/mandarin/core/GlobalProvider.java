package com.tomclaw.mandarin.core;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.tomclaw.mandarin.util.Logger;

import java.lang.reflect.Constructor;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/23/13
 * Time: 12:53 AM
 */
public class GlobalProvider extends ContentProvider {

    // Table
    public static final String REQUEST_TABLE = "requests";
    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String ROSTER_GROUP_TABLE = "roster_group";
    public static final String ROSTER_BUDDY_TABLE = "roster_buddy";
    public static final String CHAT_HISTORY_TABLE = "chat_history";
    public static final String CHAT_HISTORY_TABLE_DISTINCT = "chat_history_distinct";

    // Fields
    public static final String ROW_AUTO_ID = "_id";

    public static final String REQUEST_TYPE = "request_type";
    public static final String REQUEST_CLASS = "request_class";
    public static final String REQUEST_SESSION = "request_session";
    public static final String REQUEST_PERSISTENT = "request_persistent";
    public static final String REQUEST_ACCOUNT_DB_ID = "account_db_id";
    public static final String REQUEST_STATE = "request_state";
    public static final String REQUEST_BUNDLE = "request_bundle";
    public static final String REQUEST_TAG = "request_tag";

    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String ACCOUNT_USER_ID = "account_user_id";
    public static final String ACCOUNT_USER_PASSWORD = "account_user_password";
    public static final String ACCOUNT_STATUS = "account_status";
    public static final String ACCOUNT_STATUS_TITLE = "account_status_title";
    public static final String ACCOUNT_STATUS_MESSAGE = "account_status_message";
    public static final String ACCOUNT_CONNECTING = "account_connecting";
    public static final String ACCOUNT_BUNDLE = "account_bundle";
    public static final String ACCOUNT_AVATAR_HASH = "account_avatar_hash";

    public static final String ROSTER_GROUP_ACCOUNT_DB_ID = "account_db_id";
    public static final String ROSTER_GROUP_NAME = "group_name";
    public static final String ROSTER_GROUP_ID = "group_id";
    public static final String ROSTER_GROUP_TYPE = "group_type";
    public static final String ROSTER_GROUP_UPDATE_TIME = "group_update_time";

    public static final String GROUP_TYPE_SYSTEM = "group_system";
    public static final String GROUP_TYPE_DEFAULT = "group_default";
    public static final int GROUP_ID_RECYCLE = -1;

    public static final String ROSTER_BUDDY_ACCOUNT_DB_ID = "account_db_id";
    public static final String ROSTER_BUDDY_ACCOUNT_TYPE = "account_id";
    public static final String ROSTER_BUDDY_ID = "buddy_id";
    public static final String ROSTER_BUDDY_NICK = "buddy_nick";
    public static final String ROSTER_BUDDY_STATUS = "buddy_status";
    public static final String ROSTER_BUDDY_STATUS_TITLE = "buddy_status_title";
    public static final String ROSTER_BUDDY_STATUS_MESSAGE = "buddy_status_message";
    public static final String ROSTER_BUDDY_GROUP_ID = "buddy_group_id";
    public static final String ROSTER_BUDDY_GROUP = "buddy_group";
    public static final String ROSTER_BUDDY_DIALOG = "buddy_dialog";
    public static final String ROSTER_BUDDY_UPDATE_TIME = "buddy_update_time";
    public static final String ROSTER_BUDDY_ALPHABET_INDEX = "buddy_alphabet_index";
    public static final String ROSTER_BUDDY_UNREAD_COUNT = "buddy_unread_count";
    public static final String ROSTER_BUDDY_AVATAR_HASH = "buddy_avatar_hash";
    public static final String ROSTER_BUDDY_SEARCH_FIELD = "buddy_search_field";
    public static final String ROSTER_BUDDY_DRAFT = "buddy_draft";
    public static final String ROSTER_BUDDY_LAST_SEEN = "buddy_last_seen";
    public static final String ROSTER_BUDDY_LAST_TYPING = "buddy_last_typing";
    public static final String ROSTER_BUDDY_OPERATION = "buddy_operation";
    public static final String ROSTER_BUDDY_LAST_MESSAGE_TIME = "buddy_last_message_time";
    public static final String ROSTER_BUDDY_LAST_MESSAGE_ID = "buddy_last_message_id";
    public static final String ROSTER_BUDDY_YOURS_LAST_READ = "buddy_yours_last_read";
    public static final String ROSTER_BUDDY_THEIRS_LAST_DELIVERED = "buddy_theirs_last_delivered";
    public static final String ROSTER_BUDDY_THEIRS_LAST_READ = "buddy_theirs_last_read";
    public static final String ROSTER_BUDDY_DEL_UP_TO = "buddy_del_up_to";
    public static final String ROSTER_BUDDY_PATCH_VERSION = "buddy_patch_version";

    public static final int ROSTER_BUDDY_OPERATION_NO = 0;
    public static final int ROSTER_BUDDY_OPERATION_ADD = 1;
    public static final int ROSTER_BUDDY_OPERATION_RENAME = 2;
    public static final int ROSTER_BUDDY_OPERATION_REMOVE = 3;

    public static final String HISTORY_BUDDY_ACCOUNT_DB_ID = "account_db_id";
    public static final String HISTORY_BUDDY_ID = "buddy_id";
    public static final String HISTORY_MESSAGE_PREV_ID = "message_prev_id";
    public static final String HISTORY_MESSAGE_ID = "message_id";
    public static final String HISTORY_MESSAGE_COOKIE = "message_cookie";
    public static final String HISTORY_MESSAGE_TYPE = "message_type";
    public static final String HISTORY_MESSAGE_TIME = "message_time";
    public static final String HISTORY_MESSAGE_TEXT = "message_text";
    public static final String HISTORY_CONTENT_TYPE = "content_type";
    public static final String HISTORY_CONTENT_SIZE = "content_size";
    public static final String HISTORY_CONTENT_STATE = "content_state";
    public static final String HISTORY_CONTENT_PROGRESS = "content_progress";
    public static final String HISTORY_CONTENT_URI = "content_uri";
    public static final String HISTORY_CONTENT_NAME = "content_name";
    public static final String HISTORY_PREVIEW_HASH = "preview_hash";
    public static final String HISTORY_CONTENT_TAG = "content_tag";

    public static final int HISTORY_MESSAGE_TYPE_ERROR = 0;
    public static final int HISTORY_MESSAGE_TYPE_INCOMING = 1;
    public static final int HISTORY_MESSAGE_TYPE_OUTGOING = 2;

    public static final int HISTORY_CONTENT_TYPE_TEXT = 0;
    public static final int HISTORY_CONTENT_TYPE_PICTURE = 1;
    public static final int HISTORY_CONTENT_TYPE_VIDEO = 2;
    public static final int HISTORY_CONTENT_TYPE_FILE = 3;

    public static final int HISTORY_CONTENT_STATE_STABLE = 0;
    public static final int HISTORY_CONTENT_STATE_INTERRUPT = 1;
    public static final int HISTORY_CONTENT_STATE_STOPPED = 2;
    public static final int HISTORY_CONTENT_STATE_WAITING = 3;
    public static final int HISTORY_CONTENT_STATE_RUNNING = 4;
    public static final int HISTORY_CONTENT_STATE_FAILED = 5;

    public static final int HISTORY_MESSAGE_ID_START = 0;
    public static final int HISTORY_MESSAGE_ID_INVALID = -1;
    public static final int HISTORY_MESSAGE_ID_REQUESTED = -2;

    // Database create scripts.
    protected static final String DB_CREATE_REQUEST_TABLE_SCRIPT = "create table " + REQUEST_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, " + REQUEST_TYPE + " int, "
            + REQUEST_CLASS + " text, " + REQUEST_SESSION + " text, "
            + REQUEST_PERSISTENT + " int, " + REQUEST_ACCOUNT_DB_ID + " int, "
            + REQUEST_STATE + " int, " + REQUEST_BUNDLE + " text, " + REQUEST_TAG + " text" + ");";

    protected static final String DB_CREATE_ACCOUNT_TABLE_SCRIPT = "create table " + ACCOUNTS_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ACCOUNT_NAME + " text, " + ACCOUNT_TYPE + " text, "
            + ACCOUNT_USER_ID + " text, " + ACCOUNT_USER_PASSWORD + " text, "
            + ACCOUNT_STATUS + " int, " + ACCOUNT_STATUS_TITLE + " text, "
            + ACCOUNT_STATUS_MESSAGE + " text, " + ACCOUNT_CONNECTING + " int, "
            + ACCOUNT_BUNDLE + " text, " + ACCOUNT_AVATAR_HASH + " text" + ");";

    protected static final String DB_CREATE_GROUP_TABLE_SCRIPT = "create table " + ROSTER_GROUP_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_GROUP_ACCOUNT_DB_ID + " int, " + ROSTER_GROUP_NAME + " text, "
            + ROSTER_GROUP_ID + " text, " + ROSTER_GROUP_TYPE + " int, "
            + ROSTER_GROUP_UPDATE_TIME + " int" + ");";

    protected static final String DB_CREATE_BUDDY_TABLE_SCRIPT = "create table " + ROSTER_BUDDY_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_BUDDY_ACCOUNT_DB_ID + " int, " + ROSTER_BUDDY_ACCOUNT_TYPE + " int, "
            + ROSTER_BUDDY_ID + " text, " + ROSTER_BUDDY_NICK + " text, "
            + ROSTER_BUDDY_STATUS + " int, " + ROSTER_BUDDY_STATUS_TITLE + " text, "
            + ROSTER_BUDDY_STATUS_MESSAGE + " text, " + ROSTER_BUDDY_GROUP_ID + " int, "
            + ROSTER_BUDDY_GROUP + " text, " + ROSTER_BUDDY_DIALOG + " int, "
            + ROSTER_BUDDY_UPDATE_TIME + " int, " + ROSTER_BUDDY_ALPHABET_INDEX + " int, "
            + ROSTER_BUDDY_UNREAD_COUNT + " int default 0, " + ROSTER_BUDDY_AVATAR_HASH + " text, "
            + ROSTER_BUDDY_SEARCH_FIELD + " text, " + ROSTER_BUDDY_DRAFT + " text, "
            + ROSTER_BUDDY_LAST_SEEN + " int default -1, " + ROSTER_BUDDY_LAST_TYPING + " int default 0, "
            + ROSTER_BUDDY_OPERATION + " int default " + ROSTER_BUDDY_OPERATION_NO + ", "
            + ROSTER_BUDDY_LAST_MESSAGE_TIME + " int default 0" + ", "
            + ROSTER_BUDDY_LAST_MESSAGE_ID + " int default 0" + ", "
            + ROSTER_BUDDY_YOURS_LAST_READ + " int default 0" + ", "
            + ROSTER_BUDDY_THEIRS_LAST_DELIVERED + " int default 0" + ", "
            + ROSTER_BUDDY_THEIRS_LAST_READ + " int default 0" + ", "
            + ROSTER_BUDDY_DEL_UP_TO + " int default 0" + ", "
            + ROSTER_BUDDY_PATCH_VERSION + " text" + ");";

    protected static final String DB_CREATE_HISTORY_TABLE_SCRIPT = "create table " + CHAT_HISTORY_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + HISTORY_BUDDY_ACCOUNT_DB_ID + " int, " + HISTORY_BUDDY_ID + " text, "
            + HISTORY_MESSAGE_PREV_ID + " int, "
            + HISTORY_MESSAGE_ID + " int, "
            + HISTORY_MESSAGE_TYPE + " int, "
            + HISTORY_MESSAGE_COOKIE + " text unique, "
            + HISTORY_MESSAGE_TIME + " int, "
            + HISTORY_MESSAGE_TEXT + " text, "
            + HISTORY_CONTENT_TYPE + " int default " + HISTORY_CONTENT_TYPE_TEXT + ", "
            + HISTORY_CONTENT_SIZE + " bigint default 0, "
            + HISTORY_CONTENT_STATE + " int default " + HISTORY_CONTENT_STATE_STABLE + ", "
            + HISTORY_CONTENT_PROGRESS + " int default 0, "
            + HISTORY_CONTENT_URI + " text, " + HISTORY_CONTENT_NAME + " text, "
            + HISTORY_PREVIEW_HASH + " text, " + HISTORY_CONTENT_TAG + " text" + ");";

    protected static final String DB_CREATE_HISTORY_INDEX_BUDDY_SCRIPT = "CREATE INDEX Idx1 ON " +
            GlobalProvider.CHAT_HISTORY_TABLE + "(" +
            GlobalProvider.HISTORY_BUDDY_ID + ");";

    protected static final String DB_CREATE_HISTORY_INDEX_MESSAGE_SCRIPT = "CREATE INDEX Idx2 ON " +
            GlobalProvider.CHAT_HISTORY_TABLE + "(" +
            GlobalProvider.HISTORY_BUDDY_ID + "," +
            GlobalProvider.HISTORY_MESSAGE_TYPE + ");";

    public static final int ROW_INVALID = -1;

    // Database helper object.
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase sqLiteDatabase;

    // URI id.
    private static final int URI_REQUEST = 1;
    private static final int URI_ACCOUNT = 2;
    private static final int URI_BUDDY = 3;
    private static final int URI_GROUP = 4;
    private static final int URI_HISTORY = 5;
    private static final int URI_HISTORY_DISTINCT = 6;

    // URI tool instance.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, REQUEST_TABLE, URI_REQUEST);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ACCOUNTS_TABLE, URI_ACCOUNT);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ROSTER_GROUP_TABLE, URI_GROUP);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ROSTER_BUDDY_TABLE, URI_BUDDY);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, CHAT_HISTORY_TABLE, URI_HISTORY);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, CHAT_HISTORY_TABLE_DISTINCT, URI_HISTORY_DISTINCT);
    }

    @Override
    public boolean onCreate() {
        Logger.log("GlobalProvider onCreate");
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String table;
        boolean isDistinct = false;
        // проверяем Uri
        switch (uriMatcher.match(uri)) {
            case URI_REQUEST:
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROW_AUTO_ID + " ASC";
                }
                table = REQUEST_TABLE;
                break;
            case URI_ACCOUNT:
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ACCOUNT_NAME + " ASC";
                }
                table = ACCOUNTS_TABLE;
                break;
            case URI_GROUP:
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_GROUP_NAME + " ASC";
                }
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_BUDDY:
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_BUDDY_ID + " ASC";
                }
                table = ROSTER_BUDDY_TABLE;
                break;
            case URI_HISTORY:
                // Default sort if not specified
                table = CHAT_HISTORY_TABLE;
                break;
            case URI_HISTORY_DISTINCT:
                table = CHAT_HISTORY_TABLE;
                isDistinct = true;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        Cursor cursor;
        if (isDistinct) {
            cursor = sqLiteDatabase.query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
        } else {
            cursor = sqLiteDatabase.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        }
        cursor.setNotificationUri(getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        Logger.log("getType, " + uri.toString());
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        long rowId = sqLiteDatabase.insert(getTableName(uri), null, values);
        Uri resultUri = ContentUris.withAppendedId(uri, rowId);
        // Notify ContentResolver about data changes.
        getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        int rows = sqLiteDatabase.delete(getTableName(uri), selection, selectionArgs);
        // Notify ContentResolver about data changes.
        if (rows > 0) {
            getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        int rows = sqLiteDatabase.update(getTableName(uri), values, selection, selectionArgs);
        // Notify ContentResolver about data changes.
        if (rows > 0) {
            getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Bundle call(@NonNull String method, String arg, Bundle extras) {
        try {
            Logger.log("now proceed " + method + " database method");
            Context context = getContext();
            Class clazz = Class.forName(method);
            Constructor<DatabaseTask> constructor = clazz.getConstructor(Context.class,
                    SQLiteDatabase.class, Bundle.class);
            DatabaseTask databaseTask = constructor.newInstance(context, sqLiteDatabase, extras);
            TaskExecutor.getInstance().execute(databaseTask);
        } catch (Throwable ex) {
            Logger.log("unable to execute method " + method + " due to exception", ex);
        }
        return null;
    }

    private static String getTableName(Uri uri) {
        String table;
        switch (uriMatcher.match(uri)) {
            case URI_REQUEST:
                table = REQUEST_TABLE;
                break;
            case URI_ACCOUNT:
                table = ACCOUNTS_TABLE;
                break;
            case URI_GROUP:
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_BUDDY:
                table = ROSTER_BUDDY_TABLE;
                break;
            case URI_HISTORY:
                table = CHAT_HISTORY_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        return table;
    }

    private
    @NonNull
    ContentResolver getContentResolver() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("GlobalProvider not attached to context!");
        }
        return context.getContentResolver();
    }
}
