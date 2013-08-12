package com.tomclaw.mandarin.util;

import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.im.vk.VkAccountRoot;

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

    static {
        statuses = new HashMap<String, int[]>();
        statuses.put(IcqAccountRoot.class.getName(), IcqAccountRoot.getStatusResources());
        statuses.put(VkAccountRoot.class.getName(), IcqAccountRoot.getStatusResources());
    }

    public static int getStatusResource(String accountType, int statusIndex) {
        return statuses.get(accountType)[statusIndex];
    }
}
