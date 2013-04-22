package com.tomclaw.mandarin.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/23/13
 * Time: 12:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class RosterProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Log.d(Settings.LOG_TAG, "RosterProvider onCreate");
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getType(Uri uri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
