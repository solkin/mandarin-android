package com.tomclaw.mandarin.util;

import android.text.TextUtils;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Solkin on 28.09.2014.
 */
public class HttpParamsBuilder extends ArrayList<Pair<String, String>> {

    private static final String AMP = "&";
    private static final String EQUAL = "=";

    public HttpParamsBuilder() {
    }

    private HttpParamsBuilder(Collection<? extends Pair<String, String>> c) {
        super(c);
    }

    public HttpParamsBuilder appendParam(String key, String value) {
        add(new Pair<>(key, value));
        return this;
    }

    public HttpParamsBuilder appendParamNonEmpty(String title, String value) {
        if (!TextUtils.isEmpty(value)) {
            appendParam(title, value);
        }
        return this;
    }

    public void sortParams() {
        Collections.sort(this, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
                return lhs.first.compareTo(rhs.first);
            }
        });
    }

    /**
     * Builds Url request string from specified parameters.
     *
     * @return String - Url request parameters.
     * @throws java.io.UnsupportedEncodingException
     */
    public String build() throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        // Perform pair concatenation.
        for (Pair<String, String> pair : this) {
            if (builder.length() > 0) {
                builder.append(AMP);
            }
            builder.append(pair.first)
                    .append(EQUAL)
                    .append(StringUtil.urlEncode(pair.second));
        }
        return builder.toString();
    }

    public void reset() {
        clear();
    }

    public static HttpParamsBuilder emptyParams() {
        return new HttpParamsBuilder(Collections.<Pair<String,String>>emptyList());
    }
}
