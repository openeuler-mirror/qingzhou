package qingzhou.json;

public interface Json {
    String toJson(Object src) throws Exception;

    <T> T fromJson(String json, Class<T> classOfT) throws Exception;
}