package qingzhou.json;

import java.io.Reader;
import java.util.Properties;

import qingzhou.engine.Service;

@Service(name = "JSON Processor", description = "Provides conversion tools between JSON data and Java objects.")
public interface Json {
    String toJson(Object src);

    <T> T fromJson(String json, Class<T> classOfT);

    <T> T fromJson(Reader json, Class<T> classOfT, String... position);

    String addJson(String from, Properties toJson, String... position);

    String setJson(String from, Properties toJson, String... position);

    String deleteJson(String from, Matcher matcher, String... position);

    interface Matcher {
        boolean matches(Properties check);
    }
}
