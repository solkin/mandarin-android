package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.AccountNotFoundException;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 28.09.13
 * Time: 15:40
 */
public class HistoryDispatcher {

    private Context context;
    private NotificationManager notificationManager;
    private ContentResolver contentResolver;
    private ContentObserver historyObserver;

    public HistoryDispatcher(Context context) {
        // Variables.
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        contentResolver = context.getContentResolver();
        // Creating observers.
        historyObserver = new HistoryObserver();
    }

    public void startObservation() {
        // Registering created observers.
        contentResolver.registerContentObserver(
                Settings.HISTORY_RESOLVER_URI, true, historyObserver);
    }

    private class HistoryObserver extends ContentObserver {

        /**
         * Creates a content observer.
         */
        public HistoryObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(Settings.LOG_TAG, "HistoryObserver: onChange [selfChange = " + selfChange + "]");
            // Obtain unique unread buddies. If exist.
            String[] projection = new String[] {
                    GlobalProvider.HISTORY_BUDDY_DB_ID,
                    GlobalProvider.HISTORY_MESSAGE_TYPE,
                    GlobalProvider.HISTORY_MESSAGE_READ
            };

            StringBuilder queryBuilder = new StringBuilder();
            /*queryBuilder.append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'").append(" AND ")
                    .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'").append(" AND ")
                    .append(GlobalProvider.HISTORY_NOTICE_SHOWN).append("='").append(0).append("'");

            Cursor unShownCursor = contentResolver.query(Settings.HISTORY_DISTINCT_RESOLVER_URI, projection,
                    queryBuilder.toString(), null, null);*/

            queryBuilder = new StringBuilder();
            queryBuilder.append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'").append(" AND ")
                    .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'");

            Cursor cursor = contentResolver.query(Settings.HISTORY_DISTINCT_RESOLVER_URI, projection,
                    queryBuilder.toString(), null, null);

            if (cursor.moveToFirst()) {

                queryBuilder = new StringBuilder();
                queryBuilder.append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'").append(" AND ((")
                        .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'").append(" AND ")
                        .append(GlobalProvider.HISTORY_NOTICE_SHOWN).append("='").append(0).append("'").append(") OR ")
                        .append(GlobalProvider.HISTORY_NOTICE_SHOWN).append("='").append(-1).append("')");

                projection = new String[] {
                        GlobalProvider.HISTORY_BUDDY_DB_ID,
                        GlobalProvider.HISTORY_MESSAGE_TYPE,
                        GlobalProvider.HISTORY_MESSAGE_READ,
                        GlobalProvider.HISTORY_NOTICE_SHOWN // Нет разницы для первой и второй выборки?
                };

                Cursor unShownCursor = contentResolver.query(Settings.HISTORY_DISTINCT_RESOLVER_URI, projection,
                        queryBuilder.toString(), null, null);

                // Если есть входящие непрочитанные, то не убираем уведомление.
                // Если нет входящих непрочитанных, то уведомление убирается.
                // Показываться должны только сообщения с пометкой о непоказанности.
                if (unShownCursor.moveToFirst()) {
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    int unread = 0;
                    String content = "";

                    int HISTORY_BUDDY_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
                    // int HISTORY_BUDDY_ACCOUNT_DB_ID_COLUMN = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
                    do {
                        int buddyDbId = cursor.getInt(HISTORY_BUDDY_DB_ID_COLUMN);
                        Log.d(Settings.LOG_TAG, "HistoryObserver: buddy: " + buddyDbId);
                        StringBuilder messageQueryBuilder = new StringBuilder();
                        messageQueryBuilder.append(
                                GlobalProvider.HISTORY_BUDDY_DB_ID).append("='").append(buddyDbId).append("'")
                                .append(" AND ")
                                .append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'")
                                .append(" AND ")
                                .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'");
                        Cursor messageCursor = contentResolver.query(Settings.HISTORY_RESOLVER_URI, null,
                                messageQueryBuilder.toString(), null, GlobalProvider.ROW_AUTO_ID + " DESC");
                        if (messageCursor.moveToFirst()) {
                            int HISTORY_MESSAGE_TEXT_COLUMN = messageCursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
                            // int accountDbId = messageCursor.getInt(HISTORY_BUDDY_ACCOUNT_DB_ID_COLUMN);
                            int count = messageCursor.getCount();
                            String nickName;
                            try {
                                nickName = QueryHelper.getBuddyNick(contentResolver, buddyDbId);
                            } catch (BuddyNotFoundException ignored) {
                                nickName = "unknown";
                            }
                            if (!TextUtils.isEmpty(content)) {
                                content += ", ";
                            }
                            content += nickName;
                        /*String summary;
                        try {
                            summary = QueryHelper.getAccountName(contentResolver, accountDbId);
                        } catch (AccountNotFoundException e) {
                            summary = "unknown";
                        }*/
                            String message = messageCursor.getString(HISTORY_MESSAGE_TEXT_COLUMN);
                            Log.d(Settings.LOG_TAG, "HistoryObserver: message: " + message);

                            inboxStyle.addLine(Html.fromHtml("<b>" + nickName + "</b> " + message));
                            unread += count;

                        /*NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
                        bigText.bigText(message);
                        bigText.setBigContentTitle(title);
                        bigText.setSummaryText(summary);*/

                        /*Notification notification = new NotificationCompat.Builder(context)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setSubText(summary)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setLargeIcon(BitmapFactory.decodeResource(
                                        context.getResources(), R.drawable.ic_default_avatar))
                                .setStyle(bigText)
                                //.setStyle(inboxStyle)
                                .setNumber(count)
                                .build();
                        notificationManager.notify(buddyDbId, notification);*/
                        }
                        messageCursor.close();

                    } while (cursor.moveToNext());
                    String title = unread + " new messages";

                    inboxStyle.setBigContentTitle(title);
                    inboxStyle.setSummaryText("Mandarin");

                    Notification notification = new NotificationCompat.Builder(context)
                            .setContentTitle(title)
                            .setContentText(content)
                                    // .setSubText("Mandarin")
                            .setSmallIcon(R.drawable.ic_notification)
                            .setStyle(inboxStyle)
                                    //.setStyle(inboxStyle)
                            .setNumber(unread)
                            .build();
                    notificationManager.notify(0, notification);

                    // Plain messages modify by buddy db id and messages db id.
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);

                    /*queryBuilder = new StringBuilder();
                    queryBuilder.append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'").append(" AND ")
                            .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'").append(" AND ")
                            .append(GlobalProvider.HISTORY_NOTICE_SHOWN).append("='").append(0).append("'"); */
                    contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues, queryBuilder.toString(), null);


                    /*contentValues = new ContentValues();
                    contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 3);

                    queryBuilder = new StringBuilder();
                    queryBuilder.append(GlobalProvider.HISTORY_MESSAGE_TYPE).append("='").append(1).append("'").append(" AND ")
                            .append(GlobalProvider.HISTORY_MESSAGE_READ).append("='").append(0).append("'").append(" AND ")
                            .append(GlobalProvider.HISTORY_NOTICE_SHOWN).append("='").append(2).append("'");
                    contentResolver.update(Settings.HISTORY_RESOLVER_URI, contentValues, queryBuilder.toString(), null);*/
                } else {
                    Log.d(Settings.LOG_TAG, "HistoryObserver: No unshown messages found");
                }
                unShownCursor.close();
            } else {
                Log.d(Settings.LOG_TAG, "HistoryObserver: No unread messages found");
                notificationManager.cancel(0);
            }
            cursor.close();
        }
    }
}
