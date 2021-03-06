package com.tomclaw.mandarin.util;

import android.text.TextUtils;
import android.util.Pair;

import com.tomclaw.helpers.Strings;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
        Collections.sort(this, (lhs, rhs) -> lhs.first.compareTo(rhs.first));
    }

    /**
     * Builds Url request string from specified parameters.
     *
     * @return String - Url request parameters.
     * @throws UnsupportedEncodingException in case of encoding in incorrect
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
                    .append(Strings.urlEncode(pair.second));
        }
        return builder.toString();
    }

    public void reset() {
        clear();
    }

    public static HttpParamsBuilder emptyParams() {
        return new HttpParamsBuilder(Collections.emptyList());
    }
}
