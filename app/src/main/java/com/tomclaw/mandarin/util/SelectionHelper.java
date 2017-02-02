package com.tomclaw.mandarin.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    public void onStateChanged(K item, boolean checked) {
        if (checked) {
            setChecked(item);
        } else {
            setUnchecked(item);
        }
    }

    public void toggleChecked(K item) {
        if (isChecked(item)) {
            setUnchecked(item);
        } else {
            setChecked(item);
        }
    }

    public void setChecked(K item) {
        selection.add(item);
    }

    public boolean isChecked(K item) {
        return selection.contains(item);
    }

    public void setUnchecked(K item) {
        selection.remove(item);
    }

    public Collection<K> getSelected() {
        return Collections.unmodifiableSet(selection);
    }

    public boolean isEmptySelection() {
        return selection.isEmpty();
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
