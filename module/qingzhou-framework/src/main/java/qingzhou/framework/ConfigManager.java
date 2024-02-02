package qingzhou.framework;

import java.util.List;
import java.util.Map;

public interface ConfigManager extends InternalService {
    String publicKeyName = "publicKey";
    String privateKeyName = "privateKey";

    Map<String, String> getConfig(String index);

    List<Map<String, String>> getConfigList(String index);

    String getKey(String keyName) throws Exception;

    void writeKey(String keyName, String keyVal) throws Exception;

//    { TODO 处理这些 密钥生成的 代码，应该放在那里????
//        Callback<Void, String> DEFAULT_INIT_KEY = args -> UUID.randomUUID().toString().replace("-", "");
//
//
//        @Override
//        public String getKeyOrElseInit (File keyFile, String keyName, Callback < Void, String > initKey) throws
//        Exception {
//        if (StringUtil.isBlank(secVal)) {
//            if (initKey == null) initKey = DEFAULT_INIT_KEY;
//            secVal = initKey.run(null);
//            if (StringUtil.notBlank(secVal)) {
//                secVal = MASK_CIPHER.encrypt(secVal);
//                keyProperties.put(keyName, secVal);
//                FileUtil.writeFile(keyFile, StringUtil.propertiesToString(keyProperties));
//            }
//        }
//    }
//    }
}
