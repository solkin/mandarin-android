package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.SearchOptionsBuilder;
import com.tomclaw.mandarin.util.StringUtil;

import java.io.UnsupportedEncodingException;

/**
 * Created by Igor on 26.06.2014.
 */
public class IcqSearchOptionsBuilder extends SearchOptionsBuilder {

    private StringBuilder match;

    public IcqSearchOptionsBuilder() {
        match = new StringBuilder();
    }

    @Override
    public void keyword(String option) {
        appendOption("keyword", option);
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
        if(option) {
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

    private void appendOption(String optionName, String optionValue) {
        if(TextUtils.isEmpty(optionValue)) {
            return;
        }
        try {
            if(match.length() > 0) {
                match.append(',');
            }
            match.append(optionName).append('=').append(StringUtil.urlEncode(optionValue));
        } catch (UnsupportedEncodingException ignored) {
            // Nothing to be done in this case. Really sorry.
        }
    }

    @Override
    public String toString() {
        return match.toString();
    }

    @Override
    public boolean equals(Object o) {
        return TextUtils.equals(toString(), o.toString());
    }
}
