package com.tomclaw.mandarin.core;

import android.net.Uri;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:30 PM
 */
public class Settings {

    public static final int LED_COLOR_RGB = 0xffff6600;
    public static final int LED_BLINK_DELAY = 1000;
    public static long NOTIFICATION_MIN_DELAY = 1500;
    public static int MESSAGES_COLLAPSE_DELAY = 2 * 60 * 1000;
    public static final int TYPING_DELAY = 5 * 1000;
    public static boolean FORCE_RESTART = true;
    public static String DEVELOPER_NAME = "TomClaw Software";
    public static String LOG_TAG = "Mandarin";
    public static final String MIME_TYPE = "application/com.tomclaw.mandarin";
    public static String DB_NAME = "mandarin_db";
    public static int DB_VERSION = 7;
    public static String GLOBAL_AUTHORITY = "com.tomclaw.mandarin.core.GlobalProvider";
    public static Uri REQUEST_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.REQUEST_TABLE);
    public static Uri ACCOUNT_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.ACCOUNTS_TABLE);
    public static Uri GROUP_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.ROSTER_GROUP_TABLE);
    public static Uri BUDDY_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.ROSTER_BUDDY_TABLE);
    public static Uri HISTORY_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.CHAT_HISTORY_TABLE);
    public static Uri HISTORY_DISTINCT_RESOLVER_URI = Uri.parse("content://"
            + GLOBAL_AUTHORITY + "/" + GlobalProvider.CHAT_HISTORY_TABLE_DISTINCT);
}
