package com.tomclaw.mandarin.core;

import android.content.ContentProvider;
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
 * To change this template use File | Settings | File Templates.
 */
public class RosterProvider extends ContentProvider {

    // Table
    public static final String ROSTER_GROUP_TABLE = "roster_group";
    public static final String ROSTER_BUDDY_TABLE = "roster_buddy";

    // Fields
    private static final String ROSTER_AUTO_ID = "_id";

    public static final String ROSTER_GROUP_NAME = "group_name";

    public static final String ROSTER_BUDDY_ID = "buddy_id";
    public static final String ROSTER_BUDDY_NICK = "buddy_nick";
    public static final String ROSTER_BUDDY_STATUS = "buddy_status";
    public static final String ROSTER_BUDDY_STATE = "buddy_state";
    public static final String ROSTER_BUDDY_GROUP_ID = "buddy_group_id";
    public static final String ROSTER_BUDDY_GROUP = "buddy_group";

    // Database create scripts
    protected static final String DB_CREATE_GROUP_TABLE_SCRIPT = "create table " + ROSTER_GROUP_TABLE + "("
            + ROSTER_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_GROUP_NAME + " text" + ");";

    protected static final String DB_CREATE_BUDDY_TABLE_SCRIPT = "create table " + ROSTER_BUDDY_TABLE + "("
            + ROSTER_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_BUDDY_ID + " text, " + ROSTER_BUDDY_NICK + " text, " + ROSTER_BUDDY_STATUS + " int, "
            + ROSTER_BUDDY_STATE + " int, " + ROSTER_BUDDY_GROUP_ID + " int, " + ROSTER_BUDDY_GROUP + " text" + ");";

    // Database helper object
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase sqLiteDatabase;

    // Data types
    static final String GROUP_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + Settings.ROSTER_AUTHORITY + "." + ROSTER_GROUP_TABLE;
    static final String GROUP_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + Settings.ROSTER_AUTHORITY + "." + ROSTER_GROUP_TABLE;

    private static final int URI_BUDDY = 1;
    private static final int URI_BUDDY_ID = 2;
    private static final int URI_GROUP = 3;
    private static final int URI_GROUP_ID = 4;

    // URI tool instance
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_GROUP_TABLE, URI_GROUP);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_GROUP_TABLE + "/#", URI_GROUP_ID);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_BUDDY_TABLE, URI_BUDDY);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_BUDDY_TABLE + "/#", URI_BUDDY_ID);
    }

    @Override
    public boolean onCreate() {
        Log.d(Settings.LOG_TAG, "RosterProvider onCreate");
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(Settings.LOG_TAG, "query, " + uri.toString());
        String id;
        String table;
        // проверяем Uri
        switch (uriMatcher.match(uri)) {
            case URI_GROUP: // Default Uri
                Log.d(Settings.LOG_TAG, "URI_GROUP");
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_GROUP_NAME + " ASC";
                }
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_GROUP_ID: // Uri with ID
                id = uri.getLastPathSegment();
                Log.d(Settings.LOG_TAG, "URI_GROUP_ID, " + id);
                // добавляем ID к условию выборки
                if (TextUtils.isEmpty(selection)) {
                    selection = ROSTER_AUTO_ID + " = " + id;
                } else {
                    selection = selection + " AND " + ROSTER_AUTO_ID + " = " + id;
                }
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_BUDDY: // Default Uri
                Log.d(Settings.LOG_TAG, "URI_BUDDY");
                // Default sort if not specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_BUDDY_ID + " ASC";
                }
                table = ROSTER_BUDDY_TABLE;
                break;
            case URI_BUDDY_ID: // Uri with ID
                id = uri.getLastPathSegment();
                Log.d(Settings.LOG_TAG, "URI_BUDDY_ID, " + id);
                // добавляем ID к условию выборки
                if (TextUtils.isEmpty(selection)) {
                    selection = ROSTER_AUTO_ID + " = " + id;
                } else {
                    selection = selection + " AND " + ROSTER_AUTO_ID + " = " + id;
                }
                table = ROSTER_BUDDY_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.query(table, projection, selection,
                selectionArgs, null, null, sortOrder);
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
        switch (uriMatcher.match(uri)) {
            case URI_GROUP:
                return GROUP_CONTENT_TYPE;
            case URI_GROUP_ID:
                return GROUP_CONTENT_ITEM_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
