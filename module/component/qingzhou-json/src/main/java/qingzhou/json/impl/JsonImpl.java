package qingzhou.json.impl;

import com.google.gson.*;
import qingzhou.json.Json;

import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;

public class JsonImpl implements Json {
    @Override
    public String toJson(Object src) {
        return gsonInstance().toJson(src);
    }

    @Override
    public String addJson(String from, Properties properties, String... position) {
        JsonElement root = JsonParser.parseString(from);

        JsonObject jsonObject = root.getAsJsonObject();
        for (int i = 0; i < position.length - 1; i++) {
            jsonObject = jsonObject.get(position[i]).getAsJsonObject();
        }
        JsonArray jsonArray = jsonObject.getAsJsonArray(position[position.length - 1]);

        JsonObject addObject = new JsonObject();
        for (String key : properties.stringPropertyNames()) {
            addObject.addProperty(key, properties.getProperty(key));
        }

        if (jsonArray == null) {
            jsonArray = new JsonArray();
            jsonObject.add(position[position.length - 1], jsonArray);
        }
        jsonArray.add(addObject);

        return gsonInstance().toJson(root);
    }

    @Override
    public String setJson(String from, Properties properties, String... position) {
        JsonElement root = JsonParser.parseString(from);

        JsonObject jsonObject = root.getAsJsonObject();
        for (String path : position) {
            jsonObject = jsonObject.get(path).getAsJsonObject();
        }
        for (String key : properties.stringPropertyNames()) {
            jsonObject.addProperty(key, properties.getProperty(key));
        }

        return gsonInstance().toJson(root);
    }

    @Override
    public String setJson(String from, String value, String key, String... position) {
        JsonElement root = JsonParser.parseString(from);

        JsonObject jsonObject = root.getAsJsonObject();
        for (String path : position) {
            jsonObject = jsonObject.get(path).getAsJsonObject();
        }
        jsonObject.addProperty(key, value);
        return gsonInstance().toJson(root);
    }

    @Override
    public String deleteJson(String from, Matcher matcher, String... position) {
        JsonElement root = JsonParser.parseString(from);

        JsonObject jsonObject = root.getAsJsonObject();
        for (int i = 0; i < position.length - 1; i++) {
            jsonObject = jsonObject.get(position[i]).getAsJsonObject();
        }
        JsonArray jsonArray = jsonObject.getAsJsonArray(position[position.length - 1]);

        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            Properties properties = new Properties();
            JsonObject asJsonObject = iterator.next().getAsJsonObject();
            for (String k : asJsonObject.keySet()) {
                properties.setProperty(k, asJsonObject.get(k).getAsString());
            }
            if (matcher.matches(properties)) {
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
        JsonArray jsonArray = new JsonArray();
        for (String path : position) {
            JsonElement jsonElement = jsonObject.get(path);
            if (jsonElement == null) return null;
            if (jsonElement.isJsonArray()) {
                jsonArray = jsonElement.getAsJsonArray();
            } else {
                jsonObject = jsonElement.getAsJsonObject();
            }
        }

        if (classOfT.isArray()) {
            return gsonInstance().fromJson(jsonArray, classOfT);
        }
        return gsonInstance().fromJson(jsonObject, classOfT);
    }

    private Gson gsonInstance() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    }
}
