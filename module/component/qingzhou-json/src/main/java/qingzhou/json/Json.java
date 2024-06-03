package qingzhou.json;

import java.io.Reader;
import java.util.Properties;

public interface Json {
    String toJson(Object src);

    <T> T fromJson(String json, Class<T> classOfT);

    <T> T fromJson(Reader json, Class<T> classOfT, String... position);

    String addJson(String from, Properties properties, String... position);

    String setJson(String from, Properties properties, String... position);

    String deleteJson(String from, Matcher matcher, String... position);

    interface Matcher {
        boolean matches(Properties properties);
    }
}
