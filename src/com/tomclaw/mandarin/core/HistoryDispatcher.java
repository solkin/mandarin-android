package com.tomclaw.mandarin.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.util.QueryBuilder;

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

    private static final int NOTIFICATION_ID = 0x01;

    private static final String[] unReadProjection = new String[] {
            GlobalProvider.HISTORY_BUDDY_DB_ID,
            GlobalProvider.HISTORY_MESSAGE_TYPE,
            GlobalProvider.HISTORY_MESSAGE_READ
    };

    private static final String[] unShownProjection = new String[] {
            GlobalProvider.HISTORY_BUDDY_DB_ID,
            GlobalProvider.HISTORY_MESSAGE_TYPE,
            GlobalProvider.HISTORY_MESSAGE_READ,
            GlobalProvider.HISTORY_NOTICE_SHOWN
    };

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

        historyObserver.onChange(true);
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
            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, 1)
                    .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0);
            Cursor unReadCursor = queryBuilder.query(contentResolver, Settings.HISTORY_DISTINCT_RESOLVER_URI,
                    unReadProjection);
            // Checking for unread messages exist. If no, we must cancel notification.
            if (unReadCursor.moveToFirst()) {
                queryBuilder.recycle();
                queryBuilder.columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, 1).and().startComplexExpression()
                            .startComplexExpression()
                                .columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0)
                                .and().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, 0)
                            .finishComplexExpression()
                            .or().columnEquals(GlobalProvider.HISTORY_NOTICE_SHOWN, -1)
                        .finishComplexExpression();
                Cursor unShownCursor = queryBuilder.query(contentResolver, Settings.HISTORY_DISTINCT_RESOLVER_URI,
                        unShownProjection);
                // Checking for non-shown messages exist.
                // If yes - we must update notification with all unread messages. If no - nothing to do now.
                if (unShownCursor.moveToFirst()) {
                    boolean isAlarmRequired = false;
                    int HISTORY_NOTICE_SHOWN_COLUMN = unShownCursor.getColumnIndex(GlobalProvider.HISTORY_NOTICE_SHOWN);
                    do {
                        if(unShownCursor.getInt(HISTORY_NOTICE_SHOWN_COLUMN) != -1) {
                            isAlarmRequired = true;
                            break;
                        }
                    } while (unShownCursor.moveToNext());
                    // Notification styles for multiple and single sender respectively.
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                    // Building variables.
                    int unread = 0;
                    boolean multipleSenders = (unReadCursor.getCount() > 1);
                    StringBuilder nickNamesBuilder = new StringBuilder();
                    // Last-message variables.
                    int buddyDbId;
                    String message = "";
                    int HISTORY_BUDDY_DB_ID_COLUMN = unReadCursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_DB_ID);
                    do {
                        buddyDbId = unReadCursor.getInt(HISTORY_BUDDY_DB_ID_COLUMN);
                        QueryBuilder messageQueryBuilder = new QueryBuilder();
                        messageQueryBuilder.columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_TYPE, 1)
                                .and().columnEquals(GlobalProvider.HISTORY_MESSAGE_READ, 0);
                        messageQueryBuilder.descending(GlobalProvider.ROW_AUTO_ID);
                        Cursor messageCursor = messageQueryBuilder.query(contentResolver, Settings.HISTORY_RESOLVER_URI);
                        // Checking for the last message. Yeah, the last, not first.
                        if (messageCursor.moveToFirst()) {
                            int HISTORY_MESSAGE_TEXT_COLUMN = messageCursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
                            // Obtaining and collecting message-specific data.
                            unread += messageCursor.getCount();
                            message = messageCursor.getString(HISTORY_MESSAGE_TEXT_COLUMN);
                            String nickName;
                            try {
                                nickName = QueryHelper.getBuddyNick(contentResolver, buddyDbId);
                            } catch (BuddyNotFoundException ignored) {
                                // TODO: check with no-nick and unknown buddies.
                                nickName = "unknown";
                            }
                            if (!TextUtils.isEmpty(nickNamesBuilder)) {
                                nickNamesBuilder.append(", ");
                            }
                            nickNamesBuilder.append(nickName);
                            // Checking for style type for correct filling.
                            if(multipleSenders) {
                                inboxStyle.addLine(Html.fromHtml("<b>" + nickName + "</b> " + message));
                            }
                        }
                        messageCursor.close();
                    } while (unReadCursor.moveToNext());
                    // Common notification variables.
                    String title;
                    String content;
                    int replyIcon;
                    NotificationCompat.Style style;
                    // Checking for required style.
                    if(multipleSenders) {
                        title = context.getString(R.string.count_new_messages, unread);
                        content = nickNamesBuilder.toString();
                        replyIcon = R.drawable.social_reply_all;
                        inboxStyle.setBigContentTitle(title);
                        style = inboxStyle;
                    } else {
                        title = nickNamesBuilder.toString();
                        content = message;
                        replyIcon = R.drawable.social_reply;
                        bigTextStyle.bigText(message);
                        bigTextStyle.setBigContentTitle(title);
                        style = bigTextStyle;
                    }
                    // Show chat activity with concrete buddy.
                    PendingIntent replyNowIntent = PendingIntent.getActivity(context, 0,
                            new Intent(context, ChatActivity.class)
                                    .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    // Simply open chats list.
                    PendingIntent openChatsIntent = PendingIntent.getActivity(context, 0,
                            new Intent(context, MainActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    // Notification prepare.
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setStyle(style)
                            .addAction(replyIcon, context.getString(R.string.reply_now), replyNowIntent)
                            .addAction(R.drawable.social_chat, context.getString(R.string.open_chats), openChatsIntent)
                            .setContentIntent(multipleSenders ? openChatsIntent : replyNowIntent);
                    if(isAlarmRequired) {
                        if(PreferenceHelper.isSystemNotifications(context)) {
                            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                        } else {
                            notificationBuilder.setSound(PreferenceHelper.getNotificationUri(context));
                            int defaults = 0;
                            if(PreferenceHelper.isVibrate(context)) {
                                defaults |= Notification.DEFAULT_VIBRATE;
                            }
                            notificationBuilder.setDefaults(defaults);
                        }
                    }
                    notificationBuilder.setLights(0xffff6600, 1000, 1000);

                    Notification notification = notificationBuilder.build();
                    // Notify it right now!
                    notificationManager.notify(NOTIFICATION_ID, notification);
                    // Update shown messages flag.
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(GlobalProvider.HISTORY_NOTICE_SHOWN, 1);
                    queryBuilder.update(contentResolver, contentValues, Settings.HISTORY_RESOLVER_URI);
                } else {
                    Log.d(Settings.LOG_TAG, "HistoryObserver: Non-shown messages not found");
                }
                unShownCursor.close();
            } else {
                Log.d(Settings.LOG_TAG, "HistoryObserver: No unread messages found");
                notificationManager.cancel(NOTIFICATION_ID);
            }
            unReadCursor.close();
            // Call to update unread count.
            contentResolver.call(Settings.BUDDY_RESOLVER_URI, GlobalProvider.METHOD_UPDATE_UNREAD, null, null);
        }
    }
}
