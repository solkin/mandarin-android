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

    // Tables.
    public static final String ROSTER_GROUP_TABLE = "roster_group";
    public static final String ROSTER_BUDDY_TABLE = "roster_buddy";

    // Fields.
    private static final String ROSTER_AUTO_ID = "_id";

    public static final String ROSTER_GROUP_NAME = "group_name";

    public static final String ROSTER_BUDDY_ID = "buddy_id";
    public static final String ROSTER_BUDDY_NICK = "buddy_nick";
    public static final String ROSTER_BUDDY_STATUS = "buddy_status";
    public static final String ROSTER_BUDDY_STATE = "buddy_state";
    public static final String ROSTER_BUDDY_GROUP_ID = "buddy_group_id";
    public static final String ROSTER_BUDDY_GROUP = "buddy_group";
    public static final String ROSTER_BUDDY_DIALOG = "buddy_dialog";

    // Database create scripts.
    protected static final String DB_CREATE_GROUP_TABLE_SCRIPT = "create table " + ROSTER_GROUP_TABLE + "("
            + ROSTER_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_GROUP_NAME + " text" + ");";

    protected static final String DB_CREATE_BUDDY_TABLE_SCRIPT = "create table " + ROSTER_BUDDY_TABLE + "("
            + ROSTER_AUTO_ID + " integer primary key autoincrement, "
            + ROSTER_BUDDY_ID + " text, " + ROSTER_BUDDY_NICK + " text, " + ROSTER_BUDDY_STATUS + " int, "
            + ROSTER_BUDDY_STATE + " int, " + ROSTER_BUDDY_GROUP_ID + " int, " + ROSTER_BUDDY_GROUP + " text, "
            + ROSTER_BUDDY_DIALOG + " int" + ");";

    // Database helper object.
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase sqLiteDatabase;

    private static final int URI_BUDDY = 1;
    private static final int URI_GROUP = 2;

    // URI tool instance.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_GROUP_TABLE, URI_GROUP);
        uriMatcher.addURI(Settings.ROSTER_AUTHORITY, ROSTER_BUDDY_TABLE, URI_BUDDY);
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
        // Check URI.
        switch (uriMatcher.match(uri)) {
            case URI_GROUP:
                Log.d(Settings.LOG_TAG, "URI_GROUP");
                // Default sort if not specified.
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_GROUP_NAME + " ASC";
                }
                table = ROSTER_GROUP_TABLE;
                break;
            case URI_BUDDY:
                Log.d(Settings.LOG_TAG, "URI_BUDDY");
                // Default sort if not specified.
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ROSTER_BUDDY_ID + " ASC";
                }
                table = ROSTER_BUDDY_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.query(table, projection, selection,
                selectionArgs, null, null, sortOrder);
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
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
