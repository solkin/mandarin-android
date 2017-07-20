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

    public static ContentResolverLayer from(ContentResolver contentResolver) {
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
        return contentResolver.update(uri, contentValues, queryBuilder.getSelect(), null);
    }

    @Override
    public Cursor query(Uri uri, QueryBuilder queryBuilder) {
        return contentResolver.query(uri, null, queryBuilder.getSelect(), null, queryBuilder.getSort());
    }

    @Override
    public int delete(Uri uri, QueryBuilder queryBuilder) {
        return contentResolver.delete(uri, queryBuilder.getSelect(), null);
    }
}
