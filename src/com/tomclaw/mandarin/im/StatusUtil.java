package com.tomclaw.mandarin.im;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 4:53 PM
 */
public class StatusUtil {

    public static int STATUS_OFFLINE = 0x00;

    private static Map<String, StatusCatalogue> catalogues = new HashMap<String, StatusCatalogue>();

    public static void include(String accountType, StatusCatalogue statusCatalogue) {
        // Checking for no such status catalogue included yet.
        if (!catalogues.containsKey(accountType)) {
            catalogues.put(accountType, statusCatalogue);
        }
    }

    private static StatusCatalogue getStatusCatalogue(String accountType) {
        return catalogues.get(accountType);
    }

    private static Status getStatus(String accountType, int statusIndex) {
        return getStatusCatalogue(accountType).getStatus(statusIndex);
    }

    public static int getStatusIndex(String accountType, String statusValue) throws StatusNotFoundException {
        return getStatusCatalogue(accountType).getStatusIndex(statusValue);
    }

    public static int getStatusDrawable(String accountType, int statusIndex) {
        return getStatus(accountType, statusIndex).getDrawable();
    }

    public static String getStatusTitle(String accountType, int statusIndex) {
        return getStatus(accountType, statusIndex).getTitle();
    }

    public static String getStatusValue(String accountType, int statusIndex) {
        return getStatus(accountType, statusIndex).getValue();
    }

    public static List<Integer> getConnectStatuses(String accountType) {
        return getStatusCatalogue(accountType).getConnectStatuses();
    }
}
