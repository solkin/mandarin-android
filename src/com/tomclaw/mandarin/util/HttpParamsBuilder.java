package com.tomclaw.mandarin.util;

import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Solkin on 28.09.2014.
 */
public class HttpParamsBuilder extends ArrayList<Pair<String, String>> {

    public HttpParamsBuilder appendParam(String key, String value) {
        add(new Pair<String, String>(key, value));
        return this;
    }

    public String build() throws UnsupportedEncodingException {
        return HttpUtil.prepareParameters(this);
    }
}
