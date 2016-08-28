package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created by solkin on 17.06.15.
 */
public class ContentResolverLayer implements DatabaseLayer {

    private ContentResolver contentResolver;

    private static class Holder {

        static ContentResolverLayer instance = new ContentResolverLayer();
    }

    public static ContentResolverLayer getInstance(ContentResolver contentResolver) {
        Holder.instance.contentResolver = contentResolver;
        return Holder.instance;
    }

    private ContentResolverLayer() {
    }

    @Override
    public void insert(Uri uri, ContentValues contentValues) {
        contentResolver.insert(uri, contentValues);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, QueryBuilder queryBuilder) {
        return queryBuilder.update(contentResolver, contentValues, uri);
    }

    @Override
    public Cursor query(Uri uri, QueryBuilder queryBuilder) {
        return queryBuilder.query(contentResolver, uri);
    }

    @Override
    public int delete(Uri uri, QueryBuilder queryBuilder) {
        return queryBuilder.delete(contentResolver, uri);
    }
}
