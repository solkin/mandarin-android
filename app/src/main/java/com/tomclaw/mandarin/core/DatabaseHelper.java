package com.tomclaw.mandarin.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Random;

import static com.tomclaw.mandarin.util.StringUtil.generateRandomText;
import static com.tomclaw.mandarin.util.StringUtil.generateRandomWord;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/23/13
 * Time: 10:55 AM
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final Context context;
    private final boolean isExportDb = false;

    public DatabaseHelper(Context context) {
        super(context, Settings.DB_NAME, null, Settings.DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Creating roster database.
            db.execSQL(GlobalProvider.DB_CREATE_REQUEST_TABLE_SCRIPT);
            db.execSQL(GlobalProvider.DB_CREATE_ACCOUNT_TABLE_SCRIPT);
            db.execSQL(GlobalProvider.DB_CREATE_GROUP_TABLE_SCRIPT);
            db.execSQL(GlobalProvider.DB_CREATE_BUDDY_TABLE_SCRIPT);
            db.execSQL(GlobalProvider.DB_CREATE_HISTORY_TABLE_SCRIPT);

            db.execSQL(GlobalProvider.DB_CREATE_HISTORY_INDEX_BUDDY_SCRIPT);
            db.execSQL(GlobalProvider.DB_CREATE_HISTORY_INDEX_MESSAGE_SCRIPT);

            if (true) return;
            ContentValues cv0 = new ContentValues();
            ContentValues cv1 = new ContentValues();
            ContentValues cv2 = new ContentValues();
            ContentValues cv3 = new ContentValues();
            int[] statuses = new int[]{
                    StatusUtil.STATUS_OFFLINE
            };
            Random random = new Random(System.currentTimeMillis());
            for (int a = 0; a < 3 + random.nextInt(5); a++) {
                IcqAccountRoot accountRoot = new IcqAccountRoot();
                accountRoot.setUserId(String.valueOf(random.nextInt(999999999)));
                accountRoot.setUserPassword(generateRandomWord());
                accountRoot.setUserNick(generateRandomWord());
                cv0.put(GlobalProvider.ACCOUNT_TYPE, accountRoot.getAccountType());
                cv0.put(GlobalProvider.ACCOUNT_NAME, accountRoot.getUserNick());
                cv0.put(GlobalProvider.ACCOUNT_USER_ID, accountRoot.getUserId());
                cv0.put(GlobalProvider.ACCOUNT_USER_PASSWORD, accountRoot.getUserPassword());
                cv0.put(GlobalProvider.ACCOUNT_STATUS, accountRoot.getStatusIndex());
                cv0.put(GlobalProvider.ACCOUNT_STATUS_TITLE, generateRandomText(1 + random.nextInt(2)));
                cv0.put(GlobalProvider.ACCOUNT_STATUS_MESSAGE, generateRandomText(4 + random.nextInt(6)));
                cv0.put(GlobalProvider.ACCOUNT_CONNECTING, accountRoot.isConnecting() ? 1 : 0);
                cv0.put(GlobalProvider.ACCOUNT_BUNDLE, GsonSingleton.getInstance().toJson(accountRoot));
                long accountDbId = db.insert(GlobalProvider.ACCOUNTS_TABLE, null, cv0);
                for (int i = 1; i <= 4 + random.nextInt(3); i++) {
                    int groupId = (random.nextInt(10) == 1) ? GlobalProvider.GROUP_ID_RECYCLE : i;
                    String groupName = groupId == GlobalProvider.GROUP_ID_RECYCLE ?
                            context.getString(R.string.recycle) : generateRandomWord();
                    cv1.put(GlobalProvider.ROSTER_GROUP_ACCOUNT_DB_ID, accountDbId);
                    cv1.put(GlobalProvider.ROSTER_GROUP_NAME, groupName);
                    cv1.put(GlobalProvider.ROSTER_GROUP_ID, groupId);
                    cv1.put(GlobalProvider.ROSTER_GROUP_TYPE, GlobalProvider.GROUP_TYPE_DEFAULT);
                    db.insert(GlobalProvider.ROSTER_GROUP_TABLE, null, cv1);
                    for (int c = 1; c <= 15 + random.nextInt(15); c++) {
                        int status = statuses[random.nextInt(statuses.length)];
                        String nick = generateRandomWord();
                        String buddyId = String.valueOf(random.nextInt(999999999));
                        boolean isDialog = (random.nextInt(10) == 1);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_ACCOUNT_TYPE, accountRoot.getAccountType());
                        cv2.put(GlobalProvider.ROSTER_BUDDY_ID, buddyId);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_NICK, nick);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_GROUP, groupName);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_GROUP_ID, groupId);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_STATUS, status);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_STATUS_TITLE, generateRandomText(1 + random.nextInt(2)));
                        cv2.put(GlobalProvider.ROSTER_BUDDY_STATUS_MESSAGE, generateRandomText(4 + random.nextInt(6)));
                        cv2.put(GlobalProvider.ROSTER_BUDDY_DIALOG, isDialog);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_UPDATE_TIME, System.currentTimeMillis());
                        cv2.put(GlobalProvider.ROSTER_BUDDY_ALPHABET_INDEX, StringUtil.getAlphabetIndex(nick));
                        cv2.put(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT, 0);
                        cv2.put(GlobalProvider.ROSTER_BUDDY_SEARCH_FIELD, nick.toUpperCase());
                        long id = db.insert(GlobalProvider.ROSTER_BUDDY_TABLE, null, cv2);
                        int unreadCount = 0;
                        if (isDialog) {
                            for (int j = 0; j < random.nextInt(1500) + 1250; j++) {
                                int messageType = (random.nextInt(3) == 1) ? 2 : 1;
                                boolean isRead = (messageType != 1);
                                unreadCount += isRead ? 0 : 1;
                                cv3.put(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, accountDbId);
                                cv3.put(GlobalProvider.HISTORY_BUDDY_ID, String.valueOf(buddyId));
                                cv3.put(GlobalProvider.HISTORY_MESSAGE_TYPE, messageType);
                                cv3.put(GlobalProvider.HISTORY_MESSAGE_COOKIE, String.valueOf(random.nextLong()));
                                cv3.put(GlobalProvider.HISTORY_MESSAGE_TIME, System.currentTimeMillis() + j -
                                        24 * 60 * 60 * 1000 - 10);
                                String message = generateRandomText();
                                cv3.put(GlobalProvider.HISTORY_MESSAGE_TEXT, message);
                                db.insert(GlobalProvider.CHAT_HISTORY_TABLE, null, cv3);
                            }
                        }
                        cv2 = new ContentValues();
                        cv2.put(GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT, unreadCount);
                        //db.update(GlobalProvider.ROSTER_BUDDY_TABLE, cv2, GlobalProvider.ROW_AUTO_ID + "==" + id, null);
                    }
                }
            }

            String query = "UPDATE " + GlobalProvider.ROSTER_BUDDY_TABLE + " SET "
                    + GlobalProvider.ROSTER_BUDDY_UNREAD_COUNT + "="
                    + "(" + "SELECT COUNT(*) FROM " + GlobalProvider.CHAT_HISTORY_TABLE
                    + " WHERE " + GlobalProvider.CHAT_HISTORY_TABLE + "." + GlobalProvider.HISTORY_BUDDY_ID + "=" + GlobalProvider.ROSTER_BUDDY_TABLE + "." + GlobalProvider.ROSTER_BUDDY_ID + ");";
            Logger.log("query: " + query);
            db.execSQL(query);

            Logger.log("DB created: " + db.toString());

            PreferenceHelper.setShowStartHelper(context, false);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (isExportDb) {
            exportDb(db);
        }
    }

    private void exportDb(SQLiteDatabase db) {
        File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        FileChannel source;
        FileChannel destination;
        String currentDBPath = db.getPath();
        String backupDBPath = Settings.DB_NAME + ".db";
        File currentDB = new File(currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Yo!
        Logger.log("Now we need to upgrade database from " + oldVersion + " to " + newVersion);
        switch (oldVersion) {
            case 1: {
                db.execSQL("ALTER TABLE " + GlobalProvider.ROSTER_BUDDY_TABLE
                        + " ADD COLUMN " + GlobalProvider.ROSTER_BUDDY_DRAFT + " text");
            }
            case 2: {
                db.execSQL("ALTER TABLE " + GlobalProvider.ROSTER_BUDDY_TABLE
                        + " ADD COLUMN " + GlobalProvider.ROSTER_BUDDY_LAST_SEEN + " int default 0");
                db.execSQL("ALTER TABLE " + GlobalProvider.ROSTER_BUDDY_TABLE
                        + " ADD COLUMN " + GlobalProvider.ROSTER_BUDDY_LAST_TYPING + " int default 0");
            }
            case 3: {
                db.execSQL("ALTER TABLE " + GlobalProvider.ROSTER_BUDDY_TABLE
                        + " ADD COLUMN " + GlobalProvider.ROSTER_BUDDY_OPERATION + " int default "
                        + GlobalProvider.ROSTER_BUDDY_OPERATION_NO);
            }
            case 4: {
                db.execSQL(GlobalProvider.DB_CREATE_HISTORY_INDEX_BUDDY_SCRIPT);
                db.execSQL(GlobalProvider.DB_CREATE_HISTORY_INDEX_MESSAGE_SCRIPT);
            }
            case 5: {
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_TYPE + " int default " + GlobalProvider.HISTORY_CONTENT_TYPE_TEXT);
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_SIZE + " bigint default 0");
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_STATE + " int default " + GlobalProvider.HISTORY_CONTENT_STATE_STABLE);
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_PROGRESS + " int default 0");
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_URI + " text");
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_NAME + " text");
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_PREVIEW_HASH + " text");
                db.execSQL("ALTER TABLE " + GlobalProvider.CHAT_HISTORY_TABLE
                        + " ADD COLUMN " + GlobalProvider.HISTORY_CONTENT_TAG + " text");
            }
            case 6: {
                db.execSQL("ALTER TABLE " + GlobalProvider.ROSTER_BUDDY_TABLE
                        + " ADD COLUMN " + GlobalProvider.ROSTER_BUDDY_LAST_MESSAGE_TIME + " int default 0");
            }
        }
        Logger.log("Database upgrade completed");
    }
}
