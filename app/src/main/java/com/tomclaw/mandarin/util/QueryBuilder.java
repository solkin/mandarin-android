package com.tomclaw.mandarin.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.tomclaw.helpers.StringUtil;
import com.tomclaw.mandarin.core.DatabaseLayer;

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
        select.append(StringUtil.escapeSql(column)).append(action).append(StringUtil.escapeSqlWithQuotes(object.toString()));
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
        select.append(StringUtil.escapeSql(column)).append(" LIKE ")
                .append("'%").append(StringUtil.escapeSql(object.toString())).append("%'");
        return this;
    }

    public QueryBuilder likeIgnoreCase(String column, Object object) {
        select.append("UPPER(").append(StringUtil.escapeSql(column)).append(")").append(" LIKE ").append("'%")
                .append(StringUtil.escapeSql(object.toString())).append("%'");
        return this;
    }

    public QueryBuilder and() {
        if (!TextUtils.isEmpty(select)) {
            select.append(" AND ");
        }
        return this;
    }

    public QueryBuilder or() {
        if (!TextUtils.isEmpty(select)) {
            select.append(" OR ");
        }
        return this;
    }

    private QueryBuilder sortOrder(String column, String order) {
        sortOrderRaw(StringUtil.escapeSql(column), order);
        return this;
    }

    public QueryBuilder sortOrderRaw(String column, String order) {
        sort.append(column).append(' ').append(order);
        return this;
    }

    public QueryBuilder andOrder() {
        sort.append(", ");
        return this;
    }

    public QueryBuilder ascending(String column) {
        return sortOrder(column, "ASC");
    }

    public QueryBuilder descending(String column) {
        return sortOrder(column, "DESC");
    }

    public QueryBuilder limit(int limit) {
        sort.append(" LIMIT ").append(limit);
        return this;
    }

    public QueryBuilder startComplexExpression() {
        select.append("(");
        return this;
    }

    public QueryBuilder finishComplexExpression() {
        select.append(")");
        return this;
    }

    public Cursor query(DatabaseLayer databaseLayer, Uri uri) {
        return databaseLayer.query(uri, this);
    }

    public int delete(DatabaseLayer databaseLayer, Uri uri) {
        return databaseLayer.delete(uri, this);
    }

    public int update(DatabaseLayer databaseLayer, ContentValues contentValues, Uri uri) {
        return databaseLayer.update(uri, contentValues, this);
    }

    public CursorLoader createCursorLoader(Context context, Uri uri) {
        return new CursorLoader(context, uri, null, select.toString(), null, sort.toString());
    }

    public QueryBuilder recycle() {
        select = new StringBuilder();
        sort = new StringBuilder();
        return this;
    }

    public String getSelect() {
        return select.toString();
    }

    public String getSort() {
        return sort.toString();
    }
}
