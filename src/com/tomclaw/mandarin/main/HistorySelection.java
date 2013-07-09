package com.tomclaw.mandarin.main;

import android.text.TextUtils;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;

import java.util.*;

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
    // Current selection history adapter.
    private ChatHistoryAdapter historyAdapter;

    public HistorySelection() {
        selectionMap = new TreeMap<Integer, String>();
        selectionMode = false;
        historyAdapter = null;
    }

    public void finish() {
        // Clearing all.
        selectionMap.clear();
        selectionMode = false;
        historyAdapter = null;
    }

    public String buildSelection() {
        // Building selected messages.
        StringBuilder selectionBuilder = new StringBuilder();
        Collection<String> selection = selectionMap.values();
        for(String message : selection) {
            selectionBuilder.append(message).append('\n').append('\n');
        }
        return selectionBuilder.toString().trim();
    }

    public boolean getSelectionMode() {
        return selectionMode;
    }

    public void notifyHistoryAdapter() {
        if(historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
    }

    public void setHistoryAdapter(ChatHistoryAdapter historyAdapter) {
        this.historyAdapter = historyAdapter;
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
