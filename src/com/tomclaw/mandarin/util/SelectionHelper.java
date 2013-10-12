package com.tomclaw.mandarin.util;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.09.13
 * Time: 0:47
 */
public class SelectionHelper {

    private Map<Integer, Long> selection;

    public SelectionHelper() {
        selection = new TreeMap();
    }

    public void onStateChanged(int position, long id, boolean checked) {
        if (checked) {
            setChecked(position, id);
        } else {
            setUnchecked(position);
        }
    }

    public void setChecked(int position, long id) {
        selection.put(position, id);
    }

    public void setUnchecked(int position) {
        selection.remove(position);
    }

    public Collection<Long> getSelectedIds() {
        return selection.values();
    }

    public Collection<Integer> getSelectedPositions() {
        return selection.keySet();
    }

    public int getSelectedCount() {
        return selection.size();
    }

    public void clearSelection() {
        selection.clear();
    }
}
