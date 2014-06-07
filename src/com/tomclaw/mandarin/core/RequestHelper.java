package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import com.tomclaw.mandarin.im.icq.*;
import com.tomclaw.mandarin.util.GsonSingleton;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/12/13
 * Time: 1:30 PM
 */
public class RequestHelper {

    public static void requestMessage(ContentResolver contentResolver, int buddyDbId, String cookie, String message) {
        // Obtain account db id.
        // TODO: out this method.
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Cursor may have more than only one entry.
            // TODO: check for at least one buddy present.
            if (cursor.moveToFirst()) {
                int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                IcqMessageRequest messageRequest = new IcqMessageRequest(buddyId, message, cookie);
                insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, messageRequest);
            }
            cursor.close();
        }
    }

    public static void endSession(ContentResolver contentResolver, int accountDbId) {
        EndSessionRequest endSessionRequest = new EndSessionRequest();
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, endSessionRequest);
    }

    public static void requestBuddyAvatar(ContentResolver contentResolver, int accountDbId,
                                          String buddyId, String url) {
        // Obtain existing request.
        Cursor cursor = contentResolver.query(Settings.REQUEST_RESOLVER_URI, null,
                GlobalProvider.REQUEST_TAG + "='" + url + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Checking for at least one such download request exist.
            if (!cursor.moveToFirst()) {
                BuddyAvatarRequest buddyAvatarRequest = new BuddyAvatarRequest(buddyId, url);
                insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, true, accountDbId, buddyAvatarRequest);
            }
            cursor.close();
        }
    }

    public static void requestAccountAvatar(ContentResolver contentResolver, int accountDbId, String url) {
        // Obtain existing request.
        Cursor cursor = contentResolver.query(Settings.REQUEST_RESOLVER_URI, null,
                GlobalProvider.REQUEST_TAG + "='" + url + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Checking for at least one such download request exist.
            if (!cursor.moveToFirst()) {
                AccountAvatarRequest accountAvatarRequest = new AccountAvatarRequest(url);
                insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, true, accountDbId, accountAvatarRequest);
            }
            cursor.close();
        }
    }

    public static void requestBuddyInfo(ContentResolver contentResolver, String appSession,
                                        int accountDbId, String buddyId) {
        BuddyInfoRequest buddyInfoRequest = new BuddyInfoRequest(buddyId);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false, accountDbId, appSession, buddyInfoRequest);
    }

    public static void requestSetState(ContentResolver contentResolver, int accountDbId, int statusIndex) {
        SetStateRequest setStateRequest = new SetStateRequest(statusIndex);
        String tag = accountDbId + SetStateRequest.class.getName();
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, tag, setStateRequest);
    }

    public static void requestSetMood(ContentResolver contentResolver, int accountDbId, int statusIndex,
                                      String statusTitle, String statusMessage) {
        SetMoodRequest setMoodRequest = new SetMoodRequest(statusIndex, statusTitle, statusMessage);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, setMoodRequest);
    }

    public static void requestTyping(ContentResolver contentResolver, int buddyDbId, boolean isTyping) {
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Cursor may have more than only one entry.
            if (cursor.moveToFirst()) {
                int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                IcqTypingRequest typingRequest = new IcqTypingRequest(buddyId, isTyping);
                insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, typingRequest);
            }
            cursor.close();
        }
    }

    public static void requestRename(ContentResolver contentResolver, int accountDbId, String buddyId,
                                     String buddyPreviousNameNick, String buddySatisfiedNick) {
        BuddyRenameRequest buddyRenameRequest = new BuddyRenameRequest(
                buddyId, buddyPreviousNameNick, buddySatisfiedNick);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, buddyRenameRequest);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, boolean isPersistent,
                                      int accountDbId, String appSession, Request request) {
        insertRequest(contentResolver, type, isPersistent, accountDbId, null, appSession, request);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, boolean isPersistent,
                                      int accountDbId, Request request) {
        insertRequest(contentResolver, type, isPersistent, accountDbId, null, null, request);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, boolean isPersistent,
                                      int accountDbId, String tag, String appSession, Request request) {
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, type);
        contentValues.put(GlobalProvider.REQUEST_CLASS, request.getClass().getName());
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, isPersistent ? 1 : 0);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        if(!TextUtils.isEmpty(appSession)) {
            contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        }
        if(!TextUtils.isEmpty(tag)) {
            contentValues.put(GlobalProvider.REQUEST_TAG, tag);
        }
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(request));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }
}
