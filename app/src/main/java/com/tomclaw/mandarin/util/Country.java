package com.tomclaw.mandarin.util;

import android.text.TextUtils;

import com.tomclaw.helpers.StringUtil;

import java.util.Locale;

/**
 * Created by Solkin on 22.10.2014.
 */
public class Country implements Comparable<Country> {

    public String name;
    public int code;
    public String shortName;
    public int alphabetIndex;

    public Country(String name, int code, String shortName) {
        Locale locale = new Locale("", shortName);
        this.name = locale.getDisplayCountry();
        // Check for county not found.
        if (TextUtils.equals(this.name, shortName)) {
            this.name = name;
        }
        this.code = code;
        this.shortName = shortName;
        this.alphabetIndex = StringUtil.getAlphabetIndex(this.name);
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public String getShortName() {
        return shortName;
    }

    public int getAlphabetIndex() {
        return alphabetIndex;
    }

    @Override
    public int compareTo(Country another) {
        return name.compareTo(another.name);
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", code='+" + code + '\'' +
                '}';
    }

    public boolean contains(CharSequence constraint) {
        return toString().toLowerCase().contains(constraint.toString().toLowerCase());
    }
}
