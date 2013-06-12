package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.gson.Gson;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.util.StatusUtil;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/23/13
 * Time: 10:55 AM
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, Settings.DB_NAME, null, Settings.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creating roster database.
        db.execSQL(GlobalProvider.DB_CREATE_REQUEST_TABLE_SCRIPT);
        db.execSQL(GlobalProvider.DB_CREATE_ACCOUNT_TABLE_SCRIPT);
        db.execSQL(GlobalProvider.DB_CREATE_GROUP_TABLE_SCRIPT);
        db.execSQL(GlobalProvider.DB_CREATE_BUDDY_TABLE_SCRIPT);
        db.execSQL(GlobalProvider.DB_CREATE_HISTORY_TABLE_SCRIPT);
        ContentValues cv0 = new ContentValues();
        ContentValues cv1 = new ContentValues();
        ContentValues cv2 = new ContentValues();
        ContentValues cv3 = new ContentValues();
        int[] statuses = new int[]{
                StatusUtil.STATUS_OFFLINE,
                StatusUtil.STATUS_ONLINE,
                StatusUtil.STATUS_AWAY,
                StatusUtil.STATUS_OFFLINE,
                StatusUtil.STATUS_CHAT,
                StatusUtil.STATUS_DND,
                StatusUtil.STATUS_OFFLINE,
                StatusUtil.STATUS_MOBILE,
                StatusUtil.STATUS_OFFLINE
        };
        Random random = new Random(System.currentTimeMillis());
        Gson gson = new Gson();
        for (int a = 0; a < 3 + random.nextInt(5); a++) {
            IcqAccountRoot accountRoot = new IcqAccountRoot();
            accountRoot.setUserId(String.valueOf(random.nextInt(999999999)));
            accountRoot.setUserPassword(generateRandomWord(random));
            accountRoot.setUserNick(generateRandomWord(random));
            cv0.put(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType());
            cv0.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
            cv0.put(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
            cv0.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
            cv0.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
            cv0.put(GlobalProvider.ACCOUNT_BUNDLE, gson.toJson(accountRoot));
            long accountDbId = db.insert(GlobalProvider.ACCOUNTS_TABLE, null, cv0);
            // cv1.put(GlobalProvider.AC, groupName);
            // db.insert(GlobalProvider.ROSTER_GROUP_TABLE, null, cv1);
            for (int i = 1; i <= 4 + random.nextInt(3); i++) {
                String groupName = generateRandomWord(random);
                cv1.put(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
                cv1.put(GlobalProvider.ROSTER_GROUP_NAME, groupName);
                db.insert(GlobalProvider.ROSTER_GROUP_TABLE, null, cv1);
                for (int c = 1; c <= 5 + random.nextInt(5); c++) {
                    int status = statuses[random.nextInt(statuses.length)];
                    String nick = generateRandomWord(random);
                    boolean isDialog = (random.nextInt(10) == 1);
                    cv2.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                    cv2.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE, accountRoot.getAccountType());
                    cv2.put(GlobalProvider.ROSTER_BUDDY_ID, random.nextInt(999999999));
                    cv2.put(GlobalProvider.ROSTER_BUDDY_NICK, nick);
                    cv2.put(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
                    cv2.put(GlobalProvider.ROSTER_BUDDY_STATUS, status);
                    cv2.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isDialog /** Dialog criteria **/);
                    long id = db.insert(GlobalProvider.ROSTER_BUDDY_TABLE, null, cv2);
                    if (isDialog) {
                        for (int j = 0; j < random.nextInt(1500) + 250; j++) {
                            cv3.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
                            cv3.put(GlobalProvider.HISTORY_BUDDY_DB_ID, String.valueOf(id));
                            cv3.put(GlobalProvider.HISTORY_MESSAGE_TYPE, (random.nextInt(3) == 1) ? "2" : "1");
                            cv3.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, String.valueOf(random.nextLong()));
                            cv3.put(GlobalProvider.HISTORY_MESSAGE_STATE, "1");
                            cv3.put(GlobalProvider.HISTORY_MESSAGE_TIME, System.currentTimeMillis() + j - 24 * 60 * 60 * 1000 - 10);
                            cv3.put(GlobalProvider.HISTORY_MESSAGE_TEXT, generateRandomText(random));
                            db.insert(GlobalProvider.CHAT_HISTORY_TABLE, null, cv3);
                        }
                    }
                }
            }
        }
        Log.d(Settings.LOG_TAG, "DB created: " + db.toString());
    }

    public static String generateRandomText(Random r) {
        int wordCount = 10 + r.nextInt(13);
        StringBuilder sb = new StringBuilder(wordCount);
        for (int i = 0; i < wordCount; i++) { // For each letter in the word
            sb.append(generateRandomWord(r, i == 0)).append((i < (wordCount - 1)) ? " " : "."); // Add it to the String
        }
        return sb.toString();
    }

    private static String generateRandomWord(Random r) {
        return generateRandomWord(r, true);
    }

    private static String generateRandomWord(Random r, boolean capitalize) {
        int wordLength = 4 + r.nextInt(6);
        // Initialize a Random Number Generator with SysTime as the seed
        StringBuilder sb = new StringBuilder(wordLength);
        for (int i = 0; i < wordLength; i++) { // For each letter in the word
            char tmp = (char) ('a' + r.nextInt('z' - 'a')); // Generate a letter between a and z
            sb.append(tmp); // Add it to the String
        }
        String word = sb.toString();
        if (capitalize) {
            return String.valueOf(word.charAt(0)).toUpperCase() + word.substring(1);
        } else {
            return word;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Yo!
    }
}
