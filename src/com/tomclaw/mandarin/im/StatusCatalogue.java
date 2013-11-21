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

    protected Map<String, Integer> indexMap;
    protected Map<Integer, Status> statusMap;
    protected List<Integer> connectStatuses;

    public StatusCatalogue() {
        indexMap = new HashMap<String, Integer>();
        statusMap = new HashMap<Integer, Status>();
        connectStatuses = new ArrayList<Integer>();
    }

    public int getStatusIndex(String statusValue) throws StatusNotFoundException {
        Integer value = indexMap.get(statusValue);
        if(value == null) {
            throw new StatusNotFoundException();
        }
        return value;
    }

    public Status getStatus(int statusIndex) {
        return statusMap.get(statusIndex);
    }

    public List<Integer> getConnectStatuses() {
        return connectStatuses;
    }
}
