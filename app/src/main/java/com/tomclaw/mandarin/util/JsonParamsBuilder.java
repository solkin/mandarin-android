package com.tomclaw.mandarin;

import com.tomclaw.mandarin.util.ParamsBuilder;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class JsonParamsBuilder extends JSONObject implements ParamsBuilder {

    @Override
    public String build() throws UnsupportedEncodingException {
        return this.toString();
    }
}
