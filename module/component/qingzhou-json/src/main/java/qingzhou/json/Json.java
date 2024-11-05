package qingzhou.json;

import qingzhou.engine.ServiceInfo;

import java.io.Reader;
import java.util.Properties;

public interface Json extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to Json.";
    }

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
