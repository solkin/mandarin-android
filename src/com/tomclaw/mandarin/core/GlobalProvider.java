package com.tomclaw.mandarin.core;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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

    // Fields
    public static final String ROW_AUTO_ID = "_id";

    public static final String REQUEST_CLASS = "request_class";
    public static final String REQUEST_SESSION = "request_session";
    public static final String REQUEST_ACCOUNT = "account_db_id";
    public static final String REQUEST_BUNDLE = "request_bundle";

    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String ACCOUNT_USER_ID = "account_user_id";
    public static final String ACCOUNT_USER_PASSWORD = "account_user_password";
    public static final String ACCOUNT_STATUS = "account_status";
    public static final String ACCOUNT_BUNDLE = "account_bundle";

    public static final String ROSTER_GROUP_ACCOUNT_DB_ID = "account_db_id";
    public static final String ROSTER_GROUP_NAME = "group_name";

    public static final String ROSTER_BUDDY_ACCOUNT_DB_ID = "account_db_id";
    public static final String ROSTER_BUDDY_ACCOUNT_TYPE = "account_id";
    public static final String ROSTER_BUDDY_ID = "buddy_id";
    public static final String ROSTER_BUDDY_NICK = "buddy_nick";
    public static final String ROSTER_BUDDY_STATUS = "buddy_status";
    public static final String ROSTER_BUDDY_GROUP_ID = "buddy_group_id";
    public static final String ROSTER_BUDDY_GROUP = "buddy_group";
    public static final String ROSTER_BUDDY_DIALOG = "buddy_dialog";

    public static final String HISTORY_BUDDY_ACCOUNT_DB_ID = "account_db_id";
    public static final String HISTORY_BUDDY_DB_ID = "buddy_db_id";
    public static final String HISTORY_MESSAGE_TYPE = "message_type";
    public static final String HISTORY_MESSAGE_COOKIE = "message_cookie";
    public static final String HISTORY_MESSAGE_STATE = "message_state";
    public static final String HISTORY_MESSAGE_TIME = "message_time";
    public static final String HISTORY_MESSAGE_TEXT = "message_text";

    // Database create scripts
    protected static final String DB_CREATE_REQUEST_TABLE_SCRIPT = "create table " + REQUEST_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + REQUEST_CLASS + " text, " + REQUEST_SESSION + " text, "
            + REQUEST_ACCOUNT + " int, " + REQUEST_BUNDLE + " text" + ");";

    protected static final String DB_CREATE_ACCOUNT_TABLE_SCRIPT = "create table " + ACCOUNTS_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ACCOUNT_NAME + " text, " + ACCOUNT_TYPE + " text, "
            + ACCOUNT_USER_ID + " text, " + ACCOUNT_USER_PASSWORD + " text, " + ACCOUNT_STATUS + " text, "
            + ACCOUNT_BUNDLE + " text" + ");";

    protected static final String DB_CREATE_GROUP_TABLE_SCRIPT = "create table " + ROSTER_GROUP_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_GROUP_ACCOUNT_DB_ID + " int, " + ROSTER_GROUP_NAME + " text" + ");";

    protected static final String DB_CREATE_BUDDY_TABLE_SCRIPT = "create table " + ROSTER_BUDDY_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_BUDDY_ACCOUNT_DB_ID + " int, " + ROSTER_BUDDY_ACCOUNT_TYPE + " int, "
            + ROSTER_BUDDY_ID + " text, " + ROSTER_BUDDY_NICK + " text, "
            + ROSTER_BUDDY_STATUS + " int, " + ROSTER_BUDDY_GROUP_ID + " int, "
            + ROSTER_BUDDY_GROUP + " text, " + ROSTER_BUDDY_DIALOG + " int" + ");";

    protected static final String DB_CREATE_HISTORY_TABLE_SCRIPT = "create table " + CHAT_HISTORY_TABLE + "("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + HISTORY_BUDDY_ACCOUNT_DB_ID + " int, " + HISTORY_BUDDY_DB_ID + " int, "
            + HISTORY_MESSAGE_TYPE + " int, " + HISTORY_MESSAGE_COOKIE + " text, "
            + HISTORY_MESSAGE_STATE + " int, " + HISTORY_MESSAGE_TIME + " int, "
            + HISTORY_MESSAGE_TEXT + " text" + ");";

    // Database helper object
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase sqLiteDatabase;

    // URI id
    private static final int URI_REQUEST = 1;
    private static final int URI_ACCOUNT = 2;
    private static final int URI_BUDDY = 3;
    private static final int URI_GROUP = 4;
    private static final int URI_HISTORY = 5;

    // URI tool instance
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, REQUEST_TABLE, URI_REQUEST);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ACCOUNTS_TABLE, URI_ACCOUNT);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ROSTER_GROUP_TABLE, URI_GROUP);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, ROSTER_BUDDY_TABLE, URI_BUDDY);
        uriMatcher.addURI(Settings.GLOBAL_AUTHORITY, CHAT_HISTORY_TABLE, URI_HISTORY);
    }

    @Override
    public boolean onCreate() {
        Log.d(Settings.LOG_TAG, "GlobalProvider onCreate");
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(Settings.LOG_TAG, "query, " + uri.toString());
        String table;
        // проверяем Uri
        switch (uriMatcher.match(uri)) {
            case URI_REQUEST: // Default Uri
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROW_AUTO_ID + " ASC";
                }
                table = REQUEST_TABLE;
                break;
            case URI_ACCOUNT: // Default Uri
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ACCOUNT_NAME + " ASC";
                }
                table = ACCOUNTS_TABLE;
                break;
            case URI_GROUP: // Default Uri
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_GROUP_NAME + " ASC";
                }
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_BUDDY: // Default Uri
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_BUDDY_ID + " ASC";
                }
                table = ROSTER_BUDDY_TABLE;
                break;
            case URI_HISTORY: // Default Uri
                // Default sort if not specified
                table = CHAT_HISTORY_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        // Cursor cursor = sqLiteDatabase.query(distinct, table, projection, selection, selectionArgs, null, null, sortOrder, null);

        // Cursor cursor = sqLiteDatabase.query(true, ROSTER_GROUP_TABLE, new String[]{ROSTER_GROUP_NAME}, null, null, null, null, null, null);
        // Log.d(Settings.LOG_TAG, "Cursor items count: " + cursor.getCount());
        // просим ContentResolver уведомлять этот курсор
        // об изменениях данных в GROUP_RESOLVER_URI
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(Settings.LOG_TAG, "getType, " + uri.toString());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(Settings.LOG_TAG, "insert, " + uri.toString());
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        long rowId = sqLiteDatabase.insert(getTableName(uri), null, values);
        Uri resultUri = ContentUris.withAppendedId(uri, rowId);
        // Notify ContentResolver about data changes.
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(Settings.LOG_TAG, "delete, " + uri.toString());
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        int rows = sqLiteDatabase.delete(getTableName(uri), selection, selectionArgs);
        // Notify ContentResolver about data changes.
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(Settings.LOG_TAG, "insert, " + uri.toString());
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        int rows = sqLiteDatabase.update(getTableName(uri), values, selection, selectionArgs);
        // Notify ContentResolver about data changes.
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
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
}
