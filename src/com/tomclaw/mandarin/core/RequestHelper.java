package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import com.google.gson.Gson;
import com.tomclaw.mandarin.im.Request;
import com.tomclaw.mandarin.im.icq.EndSessionRequest;
import com.tomclaw.mandarin.im.icq.IcqMessageRequest;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/12/13
 * Time: 1:30 PM
 */
public class RequestHelper {

    private static Gson gson;

    static {
        gson = new Gson();
    }

    public static void requestMessage(ContentResolver contentResolver, String appSession,
                                      int buddyDbId, String cookie, String message) {
        // Obtain account db id.
        // TODO: out this method.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Cursor may have more than only one entry.
        // TODO: check for at least one buddy present.
        if (cursor.moveToFirst()) {
            int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
            String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
            IcqMessageRequest messageRequest = new IcqMessageRequest(buddyId, message, cookie);
            // Writing to requests database.
            ContentValues contentValues = new ContentValues();
            contentValues.put(GlobalProvider.REQUEST_CLASS, IcqMessageRequest.class.getName());
            contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
            contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
            contentValues.put(GlobalProvider.REQUEST_ACCOUNT, accountDbId);
            contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
            contentValues.put(GlobalProvider.REQUEST_BUNDLE, gson.toJson(messageRequest));
            contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
        }
        cursor.close();
    }

    public static void endSession(ContentResolver contentResolver, String appSession, int accountDbId) {
        EndSessionRequest endSessionRequest = new EndSessionRequest();
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_CLASS, EndSessionRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, gson.toJson(endSessionRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }
}
