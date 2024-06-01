package qingzhou.json.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import qingzhou.json.Json;

import java.io.Reader;

public class JsonImpl implements Json {
    @Override
    public String toJson(Object src) {
        return gsonInstance().toJson(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gsonInstance().fromJson(json, classOfT);
    }

    @Override
    public <T> T fromJson(Reader json, Class<T> classOfT) {
        return gsonInstance().fromJson(json, classOfT);
    }

    @Override
    public <T> T fromJsonMember(String json, Class<T> classOfT, String... memberPath) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return fromJsonObject(jsonObject, classOfT, memberPath);
    }

    @Override
    public <T> T fromJsonMember(Reader json, Class<T> classOfT, String... memberPath) {
        JsonObject jsonObject = JsonParser.parseReader(json).getAsJsonObject();
        return fromJsonObject(jsonObject, classOfT, memberPath);
    }

    private <T> T fromJsonObject(JsonObject jsonObject, Class<T> classOfT, String... memberPath) {
        for (String path : memberPath) {
            jsonObject = jsonObject.get(path).getAsJsonObject();
        }
        return gsonInstance().fromJson(jsonObject, classOfT);
    }

    private Gson gsonInstance() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    }
}
