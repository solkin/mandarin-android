package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.SearchOptionsBuilder;

import java.io.UnsupportedEncodingException;

/**
 * Created by Igor on 26.06.2014.
 */
public class IcqSearchOptionsBuilder extends SearchOptionsBuilder {

    private StringBuilder match;
    private String keyword;

    public IcqSearchOptionsBuilder() {
        this(0);
    }

    public IcqSearchOptionsBuilder(long searchId) {
        super(searchId);
        match = new StringBuilder();
    }

    @Override
    public void keyword(String option) {
        if (appendOption("keyword", option)) {
            keyword = option;
        }
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public void firstName(String option) {
        appendOption("firstName", option);
    }

    @Override
    public void lastName(String option) {
        appendOption("lastName", option);
    }

    @Override
    public void online(boolean option) {
        if (option) {
            appendOption("online", "true");
        }
    }

    @Override
    public void age(int from, int to) {
        appendOption("age", from + "-" + to);
    }

    @Override
    public void gender(Gender gender) {
        appendOption("gender", genderValue(gender));
    }

    public void city(String city) {
        appendOption("homeAddress.city", city);
    }

    private String genderValue(Gender gender) {
        switch (gender) {
            case Female: {
                return "female";
            }
            case Male: {
                return "male";
            }
            default: {
                return "any";
            }
        }
    }

    private boolean appendOption(String optionName, String optionValue) {
        if (TextUtils.isEmpty(optionValue)) {
            return false;
        }
        try {
            if (match.length() > 0) {
                match.append(',');
            }
            match.append(optionName).append('=').append(Strings.urlEncode(optionValue));
            return true;
        } catch (UnsupportedEncodingException ignored) {
            // Nothing to be done in this case. Really sorry.
        }
        return false;
    }

    @Override
    public String toString() {
        return match.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof SearchOptionsBuilder) {
            SearchOptionsBuilder builder = (SearchOptionsBuilder) o;
            return (getSearchId() == builder.getSearchId()) &&
                    TextUtils.equals(toString(), ((Object) builder).toString());
        }
        return false;
    }
}
