package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.tomclaw.mandarin.im.SearchOptionsBuilder;
import com.tomclaw.mandarin.im.icq.AccountAvatarRequest;
import com.tomclaw.mandarin.im.icq.BuddyAddRequest;
import com.tomclaw.mandarin.im.icq.BuddyAvatarRequest;
import com.tomclaw.mandarin.im.icq.BuddyIgnoreStateRequest;
import com.tomclaw.mandarin.im.icq.BuddyInfoRequest;
import com.tomclaw.mandarin.im.icq.BuddyPresenceRequest;
import com.tomclaw.mandarin.im.icq.BuddyRemoveRequest;
import com.tomclaw.mandarin.im.icq.BuddyRenameRequest;
import com.tomclaw.mandarin.im.icq.BuddySearchRequest;
import com.tomclaw.mandarin.im.icq.EndSessionRequest;
import com.tomclaw.mandarin.im.icq.IcqFileDownloadRequest;
import com.tomclaw.mandarin.im.icq.IcqFileUploadRequest;
import com.tomclaw.mandarin.im.icq.IcqMessageRequest;
import com.tomclaw.mandarin.im.icq.IcqSearchOptionsBuilder;
import com.tomclaw.mandarin.im.icq.IcqTypingRequest;
import com.tomclaw.mandarin.im.icq.LargeAvatarRequest;
import com.tomclaw.mandarin.im.icq.SearchAvatarRequest;
import com.tomclaw.mandarin.im.icq.SetMoodRequest;
import com.tomclaw.mandarin.im.icq.SetPermitDenyRequest;
import com.tomclaw.mandarin.im.icq.SetStateRequest;
import com.tomclaw.mandarin.im.icq.UpdateInfoRequest;
import com.tomclaw.mandarin.im.icq.UploadAvatarRequest;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.QueryBuilder;

import java.util.List;

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
                requestMessage(contentResolver, accountDbId, buddyId, cookie, message);
            }
            cursor.close();
        }
    }

    public static void requestMessage(ContentResolver contentResolver, int accountDbId, String buddyId,
                                      String cookie, String message) {
        IcqMessageRequest messageRequest = new IcqMessageRequest(buddyId, message, cookie);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, messageRequest);
    }

    public static void endSession(ContentResolver contentResolver, int accountDbId) {
        EndSessionRequest endSessionRequest = new EndSessionRequest();
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, endSessionRequest);
    }

    public static void requestBuddyAvatar(ContentResolver contentResolver, int accountDbId,
                                          String buddyId, String url) {
        requestBuddyAvatar(ContentResolverLayer.getInstance(contentResolver), accountDbId, buddyId, url);
    }

    public static void requestBuddyAvatar(SQLiteDatabase sqLiteDatabase, int accountDbId,
                                          String buddyId, String url) {
        requestBuddyAvatar(SQLiteDatabaseLayer.getInstance(sqLiteDatabase), accountDbId, buddyId, url);
    }

    public static void requestBuddyAvatar(DatabaseLayer databaseLayer, int accountDbId,
                                          String buddyId, String url) {
        BuddyAvatarRequest buddyAvatarRequest = new BuddyAvatarRequest(buddyId, url);
        insertRequest(databaseLayer, Request.REQUEST_TYPE_DOWNLOAD, accountDbId,
                url, buddyAvatarRequest);
    }

    public static void requestFileSend(ContentResolver contentResolver, int buddyDbId,
                                       String cookie, String tag, UriFile uriFile) {
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                RangedUploadRequest uploadRequest = new IcqFileUploadRequest(uriFile, buddyId, cookie);
                insertRequest(contentResolver, Request.REQUEST_TYPE_UPLOAD, accountDbId, tag, uploadRequest);
            }
            cursor.close();
        }
    }

    public static void requestFileReceive(ContentResolver contentResolver, int buddyDbId,
                                          String cookie, long time, String fileId, String fileUrl,
                                          String originalMessage, String tag) {
        Cursor cursor = contentResolver.query(Settings.BUDDY_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + buddyDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int accountDbId = cursor.getInt(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID));
                String buddyId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ROSTER_BUDDY_ID));
                RangedDownloadRequest downloadRequest = new IcqFileDownloadRequest(
                        buddyId, cookie, time, fileId, fileUrl, originalMessage, tag);
                insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, accountDbId, tag, downloadRequest);
            }
            cursor.close();
        }
    }

    public static void requestAccountAvatar(ContentResolver contentResolver, int accountDbId, String url) {
        AccountAvatarRequest accountAvatarRequest = new AccountAvatarRequest(url);
        insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, accountDbId,
                url, accountAvatarRequest);
    }

    public static void requestBuddyInfo(ContentResolver contentResolver, String appSession,
                                        int accountDbId, String buddyId) {
        BuddyInfoRequest buddyInfoRequest = new BuddyInfoRequest(buddyId);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false, accountDbId, appSession, buddyInfoRequest);
    }

    public static void requestUserInfo(ContentResolver contentResolver, String appSession, int accountDbId) {
        Cursor cursor = contentResolver.query(Settings.ACCOUNT_RESOLVER_URI, null,
                GlobalProvider.ROW_AUTO_ID + "='" + accountDbId + "'", null, null);
        // Oh, cursor may be null sometimes.
        if (cursor != null) {
            // Cursor may have more than only one entry.
            if (cursor.moveToFirst()) {
                String userId = cursor.getString(cursor.getColumnIndex(GlobalProvider.ACCOUNT_USER_ID));
                BuddyInfoRequest infoRequest = new BuddyInfoRequest(userId);
                insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false, accountDbId, appSession, infoRequest);
            }
            cursor.close();
        }
    }

    public static void requestSetState(ContentResolver contentResolver, int accountDbId, int statusIndex) {
        SetStateRequest setStateRequest = new SetStateRequest(statusIndex);
        String tag = accountDbId + SetStateRequest.class.getName();
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, true, accountDbId, tag, setStateRequest);
    }

    public static void requestSetMood(ContentResolver contentResolver, int accountDbId, int statusIndex,
                                      String statusTitle, String statusMessage) {
        SetMoodRequest setMoodRequest = new SetMoodRequest(statusIndex, statusTitle, statusMessage);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, setMoodRequest);
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
                insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, typingRequest);
            }
            cursor.close();
        }
    }

    public static void requestAdd(ContentResolver contentResolver, int accountDbId, String buddyId,
                                  String groupName, String authorizationMsg) {
        BuddyAddRequest buddyAddRequest = new BuddyAddRequest(
                buddyId, groupName, authorizationMsg);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, buddyAddRequest);
    }

    public static void requestRename(ContentResolver contentResolver, int accountDbId, String buddyId,
                                     String buddyPreviousNameNick, String buddySatisfiedNick) {
        BuddyRenameRequest buddyRenameRequest = new BuddyRenameRequest(
                buddyId, buddyPreviousNameNick, buddySatisfiedNick);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, buddyRenameRequest);
    }

    public static void requestRemove(ContentResolver contentResolver, int accountDbId,
                                     String groupName, String buddyId) {
        BuddyRemoveRequest buddyRemoveRequest = new BuddyRemoveRequest(groupName, buddyId);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, buddyRemoveRequest);
    }

    public static void requestSearch(ContentResolver contentResolver, String appSession, int accountDbId,
                                     SearchOptionsBuilder optionsBuilder, int offset) {
        BuddySearchRequest buddySearchRequest = new BuddySearchRequest((IcqSearchOptionsBuilder) optionsBuilder, 20, offset, "RU");
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false, accountDbId, appSession, buddySearchRequest);
    }

    public static void requestBuddyPresence(ContentResolver contentResolver, String appSession, int accountDbId,
                                            int total, int skipped, List<String> buddyIds,
                                            IcqSearchOptionsBuilder searchOptions) {
        BuddyPresenceRequest request = new BuddyPresenceRequest(total, skipped, buddyIds, searchOptions);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false, accountDbId, appSession, request);
    }

    public static void requestSearchAvatar(ContentResolver contentResolver, int accountDbId, String buddyId,
                                           String appSession, String url) {
        SearchAvatarRequest searchAvatarRequest = new SearchAvatarRequest(buddyId, url);
        insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, false, accountDbId,
                url, appSession, searchAvatarRequest);
    }

    public static void requestLargeAvatar(ContentResolver contentResolver, int accountDbId, String buddyId,
                                          String appSession, String url) {
        LargeAvatarRequest largeAvatarRequest = new LargeAvatarRequest(buddyId, url);
        insertRequest(contentResolver, Request.REQUEST_TYPE_DOWNLOAD, false, accountDbId,
                url, appSession, largeAvatarRequest);
    }

    public static void requestUploadAvatar(ContentResolver contentResolver, int accountDbId, String hash) {
        // Yes, this is upload operation, but is very tiny upload and be better in short queue.
        UploadAvatarRequest uploadAvatarRequest = new UploadAvatarRequest(UploadAvatarRequest.TYPE_LARGE_AVATAR, hash);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, uploadAvatarRequest);
    }

    public static void updateUserInfo(ContentResolver contentResolver, int accountDbId, String friendlyName,
                                      String firstName, String lastName, int gender, long birthDate,
                                      String city, String webSite,
                                      String aboutMe) {
        UpdateInfoRequest updateInfoRequest = new UpdateInfoRequest(friendlyName, firstName,
                lastName, gender, birthDate, city, webSite, aboutMe);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, updateInfoRequest);
    }

    public static void updateIgnoreBuddyState(ContentResolver contentResolver, int accountDbId,
                                              String buddyId, boolean ignore) {
        SetPermitDenyRequest setPermitDenyRequest = new SetPermitDenyRequest();
        if (ignore) {
            setPermitDenyRequest.setPdIgnore(buddyId);
        } else {
            setPermitDenyRequest.setPdIgnoreRemove(buddyId);
        }
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, accountDbId, setPermitDenyRequest);
    }

    public static void requestBuddyIgnoreState(ContentResolver contentResolver, String appSession,
                                               int accountDbId, String buddyId) {
        BuddyIgnoreStateRequest buddyIgnoreStateRequest = new BuddyIgnoreStateRequest(buddyId);
        insertRequest(contentResolver, Request.REQUEST_TYPE_SHORT, false,
                accountDbId, appSession, buddyIgnoreStateRequest);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, boolean isPersistent,
                                      int accountDbId, String appSession, Request request) {
        insertRequest(contentResolver, type, isPersistent, accountDbId, null, appSession, request);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, int accountDbId,
                                      Request request) {
        insertRequest(contentResolver, type, true, accountDbId, null, null, request);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, int accountDbId,
                                      String tag, Request request) {
        insertRequest(contentResolver, type, true, accountDbId, tag, null, request);
    }

    private static void insertRequest(ContentResolver contentResolver, int type, boolean isPersistent,
                                      int accountDbId, String tag, String appSession, Request request) {
        insertRequest(ContentResolverLayer.getInstance(contentResolver), type,
                isPersistent, accountDbId, tag, appSession, request);
    }

    private static void insertRequest(SQLiteDatabase sqLiteDatabase, int type, boolean isPersistent,
                                      int accountDbId, String tag, String appSession, Request request) {
        insertRequest(SQLiteDatabaseLayer.getInstance(sqLiteDatabase), type,
                isPersistent, accountDbId, tag, appSession, request);
    }

    private static void insertRequest(DatabaseLayer databaseLayer, int type, int accountDbId,
                                      String tag, Request request) {
        insertRequest(databaseLayer, type, true, accountDbId, tag, null, request);
    }

    private static void insertRequest(DatabaseLayer databaseLayer, int type, boolean isPersistent,
                                      int accountDbId, String tag, String appSession, Request request) {
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_TYPE, type);
        contentValues.put(GlobalProvider.REQUEST_CLASS, request.getClass().getName());
        contentValues.put(GlobalProvider.REQUEST_PERSISTENT, isPersistent ? 1 : 0);
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT_DB_ID, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        if (!TextUtils.isEmpty(appSession)) {
            contentValues.put(GlobalProvider.REQUEST_SESSION, appSession);
        }
        if (!TextUtils.isEmpty(tag)) {
            Cursor cursor = null;
            try {
                // Obtain existing request.
                QueryBuilder queryBuilder = new QueryBuilder()
                        .columnEquals(GlobalProvider.REQUEST_TAG, tag);
                cursor = databaseLayer.query(Settings.REQUEST_RESOLVER_URI, queryBuilder);
                // Checking for at least one such download request exist.
                if (cursor == null || cursor.moveToFirst()) {
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            contentValues.put(GlobalProvider.REQUEST_TAG, tag);
        }
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, GsonSingleton.getInstance().toJson(request));
        databaseLayer.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }

    public static void startDelayedRequest(ContentResolver contentResolver, String tag) {
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_STATE, Request.REQUEST_PENDING);
        contentResolver.update(Settings.REQUEST_RESOLVER_URI, contentValues,
                GlobalProvider.REQUEST_TAG + "='" + tag + "'", null);
    }
}
