package com.tomclaw.mandarin.util;

import android.net.Uri;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 9/28/13
 * Time: 1:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryParametersContainer {
    public Uri uri;
    public String[] projection;
    public String selection;
    public String[] selectionArgs;
    public String sortOrder;

    public QueryParametersContainer(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }
}