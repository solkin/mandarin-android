package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import com.tomclaw.mandarin.im.icq.*;
import com.tomclaw.mandarin.util.GsonSingleton;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/12/13
 * Time: 1:30 PM
 */
public class RequestHelper {

    public static void requestMessage(ContentResolver contentResolver, String appSession,
                                      int buddyDbId, String cookie, String message) {
        // Obtain account db id.
        // TODO: out this method.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if(cursor != null) {
            // Cursor may have more than only one entry.
            // TODO: check for at least one buddy present.
            if (cursor.moveToFirst()) {
                int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                IcqMessageRequest messageRequest = new IcqMessageRequest(buddyId, message, cookie);
                // Writing to requests database.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_SHORT);
                contentValues.put(GlobalProvider.REQUEST_CLASS, IcqMessageRequest.class.getName());
                contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
                contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
                contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
                contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
                contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(messageRequest));
                contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
            }
            cursor.close();
        }
    }

    public static void endSession(ContentResolver contentResolver, String appSession, int accountDbId) {
        EndSessionRequest endSessionRequest = new EndSessionRequest();
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_SHORT);
        contentValues.put(GlobalProvider.REQUEST_CLASS, EndSessionRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(endSessionRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }

    public static void requestBuddyAvatar(ContentResolver contentResolver, String appSession,
                                          int accountDbId, String buddyId, String url) {
        // Obtain existing request.
        Cursor cursor = contentResolver.query(Settings.REQUEST_RESOLVER_URI, null,
                GlobalProvider.REQUEST_TAG + "='" + url + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Checking for at least one such download request exist.
            if (!cursor.moveToFirst()) {
                BuddyAvatarRequest buddyAvatarRequest = new BuddyAvatarRequest(buddyId, url);
                // Writing to requests database.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_DOWNLOAD);
                contentValues.put(GlobalProvider.REQUEST_CLASS, BuddyAvatarRequest.class.getName());
                contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
                contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
                contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
                contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
                contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(buddyAvatarRequest));
                contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
            }
            cursor.close();
        }
    }

    public static void requestAccountAvatar(ContentResolver contentResolver, String appSession,
                                            int accountDbId, String url) {
        // Obtain existing request.
        Cursor cursor = contentResolver.query(Settings.REQUEST_RESOLVER_URI, null,
                GlobalProvider.REQUEST_TAG + "='" + url + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Checking for at least one such download request exist.
            if (!cursor.moveToFirst()) {
                AccountAvatarRequest accountAvatarRequest = new AccountAvatarRequest(url);
                // Writing to requests database.
                ContentValues contentValues = new ContentValues();
                contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_DOWNLOAD);
                contentValues.put(GlobalProvider.REQUEST_CLASS, AccountAvatarRequest.class.getName());
                contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
                contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
                contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
                contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
                contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(accountAvatarRequest));
                contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
            }
            cursor.close();
        }
    }

    public static void requestBuddyInfo(ContentResolver contentResolver, String appSession,
                                        int accountDbId, String buddyId) {
        BuddyInfoRequest buddyInfoRequest = new BuddyInfoRequest(buddyId);
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_SHORT);
        contentValues.put(GlobalProvider.REQUEST_CLASS, BuddyInfoRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 0);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(buddyInfoRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }

    public static void requestSetState(ContentResolver contentResolver, String appSession,
                                       int accountDbId, int statusIndex) {
        SetStateRequest setStateRequest = new SetStateRequest(statusIndex);
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_SHORT);
        contentValues.put(GlobalProvider.REQUEST_CLASS, SetStateRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(setStateRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }

    public static void requestSetMood(ContentResolver contentResolver, String appSession,
                                      int accountDbId, int statusIndex, String statusTitle, String statusMessage) {
        SetMoodRequest setMoodRequest = new SetMoodRequest(statusIndex, statusTitle, statusMessage);
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, Request.REQUEST_TYPE_SHORT);
        contentValues.put(GlobalProvider.REQUEST_CLASS, SetMoodRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, 1);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(setMoodRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }
}
