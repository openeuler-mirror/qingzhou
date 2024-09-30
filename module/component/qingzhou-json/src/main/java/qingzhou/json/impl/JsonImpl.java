package qingzhou.json.impl;

import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import qingzhou.json.Json;

public class JsonImpl implements Json {
    @Override
    public String toJson(Object src) {
        return gsonInstance().toJson(src);
    }

    @Override
    public String addJson(String from, Properties toJson, String... position) {
        JsonElement root = JsonParser.parseString(from);
        JsonArray jsonArray = getJsonArray(root, position);

        JsonObject addObject = new JsonObject();
        for (String k : toJson.stringPropertyNames()) {
            addObject.addProperty(k, toJson.getProperty(k));
        }

        jsonArray.add(addObject);
        return gsonInstance().toJson(root);
    }

    private JsonArray getJsonArray(JsonElement root, String... position) {
        JsonObject jsonObject = root.getAsJsonObject();
        for (int i = 0; i < position.length - 1; i++) {
            jsonObject = jsonObject.get(position[i]).getAsJsonObject();
        }
        JsonArray jsonArray = jsonObject.getAsJsonArray(position[position.length - 1]);
        if (jsonArray == null) {
            jsonArray = new JsonArray();
            jsonObject.add(position[position.length - 1], jsonArray);
        }
        return jsonArray;
    }

    @Override
    public String setJson(String from, Properties toJson, String... position) {
        JsonElement root = JsonParser.parseString(from);

        JsonObject jsonObject = root.getAsJsonObject();
        for (String path : position) {
            jsonObject = jsonObject.get(path).getAsJsonObject();
        }
        for (String k : toJson.stringPropertyNames()) {
            jsonObject.addProperty(k, toJson.getProperty(k));
        }

        return gsonInstance().toJson(root);
    }

    @Override
    public String deleteJson(String from, Matcher matcher, String... position) {
        JsonElement root = JsonParser.parseString(from);
        JsonArray jsonArray = getJsonArray(root, position);

        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            Properties check = new Properties();
            JsonObject asJsonObject = iterator.next().getAsJsonObject();
            for (String k : asJsonObject.keySet()) {
                check.put(k, asJsonObject.get(k).getAsString());
            }
            if (matcher.matches(check)) {
                iterator.remove();
            }
        }

        return gsonInstance().toJson(root);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gsonInstance().fromJson(json, classOfT);
    }

    @Override
    public <T> T fromJson(Reader from, Class<T> classOfT, String... position) {
        JsonElement root = JsonParser.parseReader(from);
        JsonObject jsonObject = root.getAsJsonObject();
        JsonArray jsonArray = null;
        for (int i = 0; i < position.length; i++) {
            String path = position[i];
            JsonElement jsonElement = jsonObject.get(path);
            if (jsonElement.isJsonArray()) {
                if (i != position.length - 1) { // 数组类型仅限位于最后一层
                    throw new IllegalStateException();
                }
                jsonArray = jsonElement.getAsJsonArray();
            } else {
                jsonObject = jsonElement.getAsJsonObject();
            }
        }

        if (classOfT.isArray()) {
            return gsonInstance().fromJson(jsonArray, classOfT);
        } else {
            return gsonInstance().fromJson(jsonObject, classOfT);
        }
    }

    private Gson gsonInstance() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    }
}
