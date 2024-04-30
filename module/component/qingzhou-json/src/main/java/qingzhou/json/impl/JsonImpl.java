package qingzhou.json.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    @Override
    public <T> T fromJsonMember(String json, String memberName, Class<T> classOfT) {
        JsonObject asJsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonElement jsonElement = asJsonObject.get(memberName);
        Gson gson = new Gson();
        return gson.fromJson(jsonElement, classOfT);
    }
}
