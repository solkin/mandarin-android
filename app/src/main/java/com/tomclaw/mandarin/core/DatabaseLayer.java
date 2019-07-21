package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created by solkin on 17.06.15.
 */
public interface DatabaseLayer {

    void insert(Uri uri, ContentValues contentValues);

    int update(Uri uri, ContentValues contentValues, QueryBuilder queryBuilder);

    Cursor query(Uri uri, QueryBuilder queryBuilder);

    int delete(Uri uri, QueryBuilder queryBuilder);
}
