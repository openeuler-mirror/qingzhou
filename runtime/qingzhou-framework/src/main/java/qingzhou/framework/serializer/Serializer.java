package qingzhou.framework.serializer;

public interface Serializer {
    byte[] serialize(Object obj) throws Exception;

    <T> T deserialize(byte[] bytes, Class<T> tClass) throws Exception; // 安全起见，指定具体的类型，避免反序列化漏洞
}
