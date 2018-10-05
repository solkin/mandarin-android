package com.tomclaw.mandarin.util;

import com.google.gson.Gson;

/**
 * Created by solkin on 01/03/14.
 */
public class GsonSingleton {

    private static class Holder {

        static GsonSingleton instance = new GsonSingleton();
    }

    public static GsonSingleton getInstance() {
        return Holder.instance;
    }

    private Gson gson;

    private GsonSingleton() {
        gson = new Gson();
    }

    public Gson getGson() {
        return gson;
    }

    public String toJson(Object object) {
        return gson.toJson(object);
    }

    public <T> T fromJson(java.lang.String json, java.lang.Class<T> classOfT)
            throws com.google.gson.JsonSyntaxException {
        return gson.fromJson(json, classOfT);
    }
}
