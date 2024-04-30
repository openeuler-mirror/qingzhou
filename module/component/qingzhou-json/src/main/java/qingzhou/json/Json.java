package qingzhou.json;

public interface Json {
    String toJson(Object src);

    <T> T fromJson(String json, Class<T> classOfT);

    <T> T fromJsonMember(String json, String memberName, Class<T> classOfT);
}
