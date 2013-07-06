package com.tomclaw.mandarin.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 7/5/13
 * Time: 9:24 PM
 */
public class HistorySelection {

    private static class Holder {

        static HistorySelection instance = new HistorySelection();
    }

    public static HistorySelection getInstance() {
        return Holder.instance;
    }

    private Map<Integer, Boolean> selectionMap;
    private boolean selectionMode;

    public HistorySelection() {
        selectionMap = new HashMap<Integer, Boolean>();
        selectionMode = false;
    }

    public Set<Integer> complete() {
        Set<Integer> positions = selectionMap.keySet();
        // Clearing all.
        selectionMap.clear();
        selectionMode = false;
        return positions;
    }

    public boolean getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }

    public void setSelection(int position, boolean value) {
        if(value) {
            selectionMap.put(position, value);
        } else if(selectionMap.containsKey(position)) {
            selectionMap.remove(position);
        }
    }

    public boolean getSelection(int position) {
        if(selectionMap.containsKey(position)) {
            return selectionMap.get(position);
        }
        return false;
    }
}
