package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import com.google.gson.Gson;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.icq.MessageRequest;

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

    public static void requestMessage(ContentResolver contentResolver, int accountDbId, int buddyDbId,
                                      String cookie, String message) {
        MessageRequest messageRequest = new MessageRequest();
        // Writing to requests database.
        ContentValues contentValues = new ContentValues();
        contentValues.put(GlobalProvider.REQUEST_CLASS, MessageRequest.class.getName());
        contentValues.put(GlobalProvider.REQUEST_SESSION, System.currentTimeMillis());
        contentValues.put(GlobalProvider.REQUEST_ACCOUNT, accountDbId);
        contentValues.put(GlobalProvider.REQUEST_BUNDLE, gson.toJson(messageRequest));
        contentResolver.insert(Settings.REQUEST_RESOLVER_URI, contentValues);
    }
}
