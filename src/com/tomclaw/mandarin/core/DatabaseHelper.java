package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.tomclaw.mandarin.R;

import java.util.Random;

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
        db.execSQL(DataProvider.DB_CREATE_GROUP_TABLE_SCRIPT);
        db.execSQL(DataProvider.DB_CREATE_BUDDY_TABLE_SCRIPT);
        ContentValues cv = new ContentValues();
        ContentValues cv1 = new ContentValues();
        int[] statuses = new int[]{
                R.drawable.status_icq_offline,
                R.drawable.status_icq_online,
                R.drawable.status_icq_away,
                R.drawable.status_icq_offline,
                R.drawable.status_icq_chat,
                R.drawable.status_icq_dnd,
                R.drawable.status_icq_offline,
                R.drawable.status_icq_mobile,
                R.drawable.status_icq_offline
        };
        Random random = new Random(System.currentTimeMillis());
        for (int i = 1; i <= 30; i++) {
            String groupName = generateRandomWord(random);
            cv.put(DataProvider.ROSTER_GROUP_NAME, groupName);
            db.insert(DataProvider.ROSTER_GROUP_TABLE, null, cv);
            for (int c = 1; c <= 40; c++) {
                int status = statuses[random.nextInt(statuses.length)];
                cv1.put(DataProvider.ROSTER_BUDDY_ID, generateRandomWord(random) + "@molecus.com");
                cv1.put(DataProvider.ROSTER_BUDDY_NICK, generateRandomWord(random));
                cv1.put(DataProvider.ROSTER_BUDDY_GROUP, groupName);
                cv1.put(DataProvider.ROSTER_BUDDY_STATUS, status);
                cv1.put(DataProvider.ROSTER_BUDDY_STATE, status != R.drawable.status_icq_offline);
                cv1.put(DataProvider.ROSTER_BUDDY_DIALOG, random.nextInt(50) == 1 /** Online criteria **/);
                db.insert(DataProvider.ROSTER_BUDDY_TABLE, null, cv1);
            }
        }
        Log.d(Settings.LOG_TAG, "DB created: " + db.toString());
    }

    private String generateRandomWord(Random r) {
        int wordLength = 4 + r.nextInt(6);
        // Intialize a Random Number Generator with SysTime as the seed
        StringBuilder sb = new StringBuilder(wordLength);
        for (int i = 0; i < wordLength; i++) { // For each letter in the word
            char tmp = (char) ('a' + r.nextInt('z' - 'a')); // Generate a letter between a and z
            sb.append(tmp); // Add it to the String
        }
        String word = sb.toString();
        return String.valueOf(word.charAt(0)).toUpperCase() + word.substring(1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
