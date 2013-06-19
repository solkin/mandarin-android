package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.util.StatusUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/19/13
 * Time: 9:49 PM
 */
public final class IcqStatusUtil {

    public static Map<String, Integer> statusMap;

    static {
        statusMap = new HashMap<String, Integer>();
        statusMap.put("offline", StatusUtil.STATUS_OFFLINE);
        statusMap.put("mobile", StatusUtil.STATUS_MOBILE);
        statusMap.put("online", StatusUtil.STATUS_ONLINE);
        statusMap.put("invisible", StatusUtil.STATUS_INVISIBLE);
        statusMap.put("away", StatusUtil.STATUS_AWAY);
        statusMap.put("occupied", StatusUtil.STATUS_DND);
        statusMap.put("na", StatusUtil.STATUS_NA);
        statusMap.put("busy", StatusUtil.STATUS_BUSY);

        statusMap.put("notFound", StatusUtil.STATUS_OFFLINE);
        statusMap.put("unknown", StatusUtil.STATUS_OFFLINE);
        statusMap.put("idle", StatusUtil.STATUS_INVISIBLE);
    }

    public static final int getStatusIndex(String status) {
        return statusMap.get(status);
    }
}
