package com.tomclaw.mandarin.im;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/21/13
 * Time: 12:23 AM
 */
public abstract class StatusCatalogue {

    private static final int STATUS_INDEX_INVALID = -1;

    protected Map<String, Integer> indexMap;
    protected List<Status> statusList;
    protected List<Integer> connectStatuses;
    protected List<Integer> setupStatuses;
    protected int musicStatus;

    public StatusCatalogue() {
        indexMap = new HashMap<String, Integer>();
        statusList = new ArrayList<Status>();
        connectStatuses = new ArrayList<Integer>();
        setupStatuses = new ArrayList<Integer>();
        musicStatus = STATUS_INDEX_INVALID;
    }

    public int getStatusIndex(String statusValue) throws StatusNotFoundException {
        Integer value = indexMap.get(statusValue);
        if (value == null) {
            throw new StatusNotFoundException();
        }
        return value;
    }

    public Status getStatus(int statusIndex) {
        return statusList.get(statusIndex);
    }

    public List<Integer> getConnectStatuses() {
        return connectStatuses;
    }

    public List<Integer> getSetupStatuses() {
        return setupStatuses;
    }

    public int getMusicStatus() throws StatusNotFoundException {
        if (musicStatus == STATUS_INDEX_INVALID) {
            throw new StatusNotFoundException();
        }
        return musicStatus;
    }
}
