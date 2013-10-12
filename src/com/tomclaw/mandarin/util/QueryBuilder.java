package com.tomclaw.mandarin.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 03.10.13
 * Time: 11:09
 */
public class QueryBuilder {

    private StringBuilder select;
    private StringBuilder sort;

    public QueryBuilder() {
        recycle();
    }

    private QueryBuilder expression(String column, String action, Object object) {
        select.append(column).append(action).append("'").append(object).append("'");
        return this;
    }

    public QueryBuilder columnEquals(String column, Object object) {
        return expression(column, "=", object);
    }

    public QueryBuilder columnNotEquals(String column, Object object) {
        return expression(column, "!=", object);
    }

    public QueryBuilder more(String column, Object object) {
        return expression(column, ">", object);
    }

    public QueryBuilder less(String column, Object object) {
        return expression(column, "<", object);
    }

    public QueryBuilder moreOrEquals(String column, Object object) {
        return expression(column, ">=", object);
    }

    public QueryBuilder lessOrEquals(String column, Object object) {
        return expression(column, "<=", object);
    }

    public QueryBuilder like(String column, Object object) {
        select.append(column).append(" LIKE ").append("'%").append(object).append("%'");
        return this;
    }

    public QueryBuilder and() {
        select.append(" AND ");
        return this;
    }

    public QueryBuilder or() {
        select.append(" OR ");
        return this;
    }

    private QueryBuilder sortOrder(String column, String order) {
        sort.append(column).append(order);
        return this;
    }

    public QueryBuilder ascending(String column) {
        return sortOrder(column, " ASC");
    }

    public QueryBuilder descending(String column) {
        return sortOrder(column, " DESC");
    }

    public QueryBuilder limit(int limit) {
        sort.append(" LIMIT ").append(limit);
        return this;
    }

    public Cursor query(ContentResolver contentResolver, Uri uri) {
        return contentResolver.query(uri, null, select.toString(), null, sort.toString());
    }

    public int delete(ContentResolver contentResolver, Uri uri) {
        return contentResolver.delete(uri, select.toString(), null);
    }

    public int update(ContentResolver contentResolver, ContentValues contentValues, Uri uri) {
        return contentResolver.update(uri, contentValues, select.toString(), null);
    }

    public QueryBuilder recycle() {
        select = new StringBuilder();
        sort = new StringBuilder();
        return this;
    }
}
