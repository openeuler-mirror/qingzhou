package qingzhou.config;


import java.util.List;
import java.util.Map;

public interface Config {
    String localKeyName = "localKey";
    String remoteKeyName = "remoteKey";
    String remotePublicKeyName = "remotePublicKey"; // TODO: 节点使用，用于和集中管理通信加密

    String publicKeyName = "publicKey";
    String privateKeyName = "privateKey";

    boolean existsConfig(String index);

    Map<String, String> getConfig(String index);

    List<Map<String, String>> getConfigList(String index);

    void addConfig(String parentIndex, String flag, Map<String, String> properties);

    void updateConfig(String index, Map<String, String> config);

    void deleteConfig(String index);

    String getKey(String keyName) throws Exception;

    void writeKey(String keyName, String keyVal) throws Exception;
}
