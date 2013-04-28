package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/23/13
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, Settings.DB_NAME, null, Settings.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creating roster database.
        db.execSQL(RosterProvider.DB_CREATE_GROUP_TABLE_SCRIPT);
        db.execSQL(RosterProvider.DB_CREATE_BUDDY_TABLE_SCRIPT);
        ContentValues cv = new ContentValues();
        ContentValues cv1 = new ContentValues();
        for (int i = 1; i <= 30; i++) {
            String groupName = "Group " + i;
            cv.put(RosterProvider.ROSTER_GROUP_NAME, groupName);
            db.insert(RosterProvider.ROSTER_GROUP_TABLE, null, cv);
            for (int c=1;c<=40;c++){
                cv1.put(RosterProvider.ROSTER_BUDDY_ID, "buddy" + c + "@molecus.com");
                cv1.put(RosterProvider.ROSTER_BUDDY_NICK, "Buddy " + c + " from " + groupName);
                cv1.put(RosterProvider.ROSTER_BUDDY_GROUP, groupName);
                db.insert(RosterProvider.ROSTER_BUDDY_TABLE, null, cv1);
            }
        }
        Log.d(Settings.LOG_TAG, "DB created: " + db.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
