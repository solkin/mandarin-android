package com.tomclaw.mandarin.util;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.09.13
 * Time: 0:47
 */
public class SelectionHelper<K> {

    private Set<K> selection;
    private boolean isSelectionMode;

    public SelectionHelper() {
        selection = new HashSet<>();
        isSelectionMode = false;
    }

    public void onStateChanged(K id, boolean checked) {
        if (checked) {
            setChecked(id);
        } else {
            setUnchecked(id);
        }
    }

    public void toggleChecked(K id) {
        if (isChecked(id)) {
            setUnchecked(id);
        } else {
            setChecked(id);
        }
    }

    public void setChecked(K id) {
        selection.add(id);
    }

    public boolean isChecked(K id) {
        return selection.contains(id);
    }

    public void setUnchecked(K id) {
        selection.remove(id);
    }

    public Collection<K> getSelectedIds() {
        return Collections.unmodifiableSet(selection);
    }

    public int getSelectedCount() {
        return selection.size();
    }

    public void clearSelection() {
        selection.clear();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public boolean setSelectionMode(boolean isSelectionMode) {
        if (this.isSelectionMode != isSelectionMode) {
            this.isSelectionMode = isSelectionMode;
            clearSelection();
            return true;
        }
        return false;
    }
}
