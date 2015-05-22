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
public class SelectionHelper<K, V> {

    private Map<K, V> selection;

    public SelectionHelper() {
        selection = new TreeMap<K, V>();
    }

    public void onStateChanged(K position, V id, boolean checked) {
        if (checked) {
            setChecked(position, id);
        } else {
            setUnchecked(position);
        }
    }

    public void setChecked(K position, V id) {
        selection.put(position, id);
    }

    public boolean isChecked(K position) {
        return selection.containsKey(position);
    }

    public void setUnchecked(K position) {
        selection.remove(position);
    }

    public Collection<V> getSelectedIds() {
        return selection.values();
    }

    public Collection<K> getSelectedPositions() {
        return selection.keySet();
    }

    public int getSelectedCount() {
        return selection.size();
    }

    public void clearSelection() {
        selection.clear();
    }
}
