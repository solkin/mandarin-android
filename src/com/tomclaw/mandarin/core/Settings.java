package com.tomclaw.mandarin.core;

import android.net.Uri;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/14/13
 * Time: 9:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Settings {

    public static String LOG_TAG = "Mandarin";
    public static String DB_NAME = "mandarin_db";
    public static int DB_VERSION = 1;
    public static String ROSTER_AUTHORITY = "com.tomclaw.mandarin.core.RosterProvider";
    public static Uri GROUP_RESOLVER_URI = Uri.parse("content://"
            + ROSTER_AUTHORITY + "/" + RosterProvider.ROSTER_GROUP_TABLE);
    public static Uri BUDDY_RESOLVER_URI = Uri.parse("content://"
            + ROSTER_AUTHORITY + "/" + RosterProvider.ROSTER_BUDDY_TABLE);
}
