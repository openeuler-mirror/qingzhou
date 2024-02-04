package qingzhou.framework;

import java.util.List;
import java.util.Map;

public interface ConfigManager extends InternalService {
    String localKeyName = "localKey";
    String remoteKeyName = "remoteKey";
    String remotePublicKeyName = "remotePublicKey"; // 节点使用，用于和集中管理通信加密

    String publicKeyName = "publicKey";
    String privateKeyName = "privateKey";

    Map<String, String> getConfig(String index);

    List<Map<String, String>> getConfigList(String index);

    String getKey(String keyName) throws Exception;

    void writeKey(String keyName, String keyVal) throws Exception;
}
