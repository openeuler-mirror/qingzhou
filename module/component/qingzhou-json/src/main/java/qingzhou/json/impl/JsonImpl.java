package qingzhou.json.impl;

import com.google.gson.Gson;
import qingzhou.json.Json;

public class JsonImpl implements Json {
    @Override
    public String toJson(Object src) {
        Gson gson = new Gson();
        return gson.toJson(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        Gson gson = new Gson();
        return gson.fromJson(json, classOfT);
    }
}
