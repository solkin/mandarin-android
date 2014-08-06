package com.tomclaw.mandarin.im;

import java.io.Serializable;

/**
 * Created by Igor on 26.06.2014.
 */
public abstract class SearchOptionsBuilder implements Serializable {

    private final long searchId;

    public SearchOptionsBuilder(long searchId) {
        this.searchId = searchId;
    }

    public abstract void keyword(String option);

    public abstract void firstName(String option);

    public abstract void lastName(String option);

    public abstract void online(boolean option);

    public abstract void age(int from, int to);

    public abstract void gender(Gender gender);

    public long getSearchId() {
        return searchId;
    }
}
