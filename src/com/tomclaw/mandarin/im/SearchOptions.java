package com.tomclaw.mandarin.im;

/**
 * Created by solkin on 09/03/14.
 */
public class SearchOptions {

    private String keyword;
    private int gender;
    private int ageFrom;
    private int ageTo;
    private boolean onlineOnly;
    private int skipCount;
    private int getCount;

    protected SearchOptions(String keyword, int gender, int ageFrom, int ageTo, boolean onlineOnly,
                            int skipCount, int getCount) {
        this.keyword = keyword;
        this.gender = gender;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.onlineOnly = onlineOnly;
        this.skipCount = skipCount;
        this.getCount = getCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getGender() {
        return gender;
    }

    public int getAgeFrom() {
        return ageFrom;
    }

    public int getAgeTo() {
        return ageTo;
    }

    public boolean isOnlineOnly() {
        return onlineOnly;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public int getGetCount() {
        return getCount;
    }
}
