package com.tomclaw.mandarin.util;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 4:53 PM
 */
public class StatusUtil {

    public static int STATUS_OFFLINE = 0x00;
    public static int STATUS_MOBILE = 0x01;
    public static int STATUS_ONLINE = 0x02;
    public static int STATUS_INVISIBLE = 0x03;
    public static int STATUS_CHAT = 0x04;
    public static int STATUS_AWAY = 0x05;
    public static int STATUS_DND = 0x06;
    public static int STATUS_NA = 0x07;
    public static int STATUS_BUSY = 0x08;

    private static Map<String, int[]> statuses;
    private static Map<String, Integer[]> connectStatuses;
    private static int[] statusStrings;

    static {
        statuses = new HashMap<String, int[]>();
        statuses.put(IcqAccountRoot.class.getName(), IcqAccountRoot.getStatusDrawables());

        connectStatuses = new HashMap<String, Integer[]>();
        connectStatuses.put(IcqAccountRoot.class.getName(), IcqAccountRoot.getConnectStatuses());

        statusStrings = new int[] {
                R.string.status_offline,
                R.string.status_mobile,
                R.string.status_online,
                R.string.status_invisible,
                R.string.status_chat,
                R.string.status_away,
                R.string.status_dnd,
                R.string.status_na,
                R.string.status_busy
        };
    }

    public static int getStatusResource(String accountType, int statusIndex) {
        return statuses.get(accountType)[statusIndex];
    }

    public static Integer[] getConnectStatuses(String accountType) {
        return connectStatuses.get(accountType);
    }

    public static int getStatusTitle(int statusIndex) {
        return statusStrings[statusIndex];
    }
}
