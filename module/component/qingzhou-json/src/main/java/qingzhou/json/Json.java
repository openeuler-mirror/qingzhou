package qingzhou.json;

import java.io.Reader;

public interface Json {
    String toJson(Object src);

    <T> T fromJson(String json, Class<T> classOfT);

    <T> T fromJson(Reader json, Class<T> classOfT);

    <T> T fromJsonMember(String json, Class<T> classOfT, String... memberPath);

    <T> T fromJsonMember(Reader json, Class<T> classOfT, String... memberPath);
}
