package com.tomclaw.mandarin.main;

import android.text.TextUtils;
import com.actionbarsherlock.view.CollapsibleActionView;

import java.util.Collection;
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

    private Map<Integer, String> selectionMap;
    private boolean selectionMode;

    public HistorySelection() {
        selectionMap = new HashMap<Integer, String>();
        selectionMode = false;
    }

    public void finish() {
        // Clearing all.
        selectionMap.clear();
        selectionMode = false;
    }

    public String buildSelection() {
        // Building selected messages.
        StringBuilder selectionBuilder = new StringBuilder();
        Collection<String> selection = selectionMap.values();
        for(String message : selection) {
            selectionBuilder.append(message).append('\n').append('\n');
        }
        return selectionBuilder.toString();
    }

    public boolean getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }

    public void setSelection(int position, String value) {
        if(TextUtils.isEmpty(value)) {
            selectionMap.remove(position);
        } else {
            selectionMap.put(position, value);
        }
    }

    public String getSelection(int position) {
        if(selectionMap.containsKey(position)) {
            return selectionMap.get(position);
        }
        return null;
    }

    public boolean isSelectionExist(int position) {
        return !TextUtils.isEmpty(getSelection(position));
    }
}
