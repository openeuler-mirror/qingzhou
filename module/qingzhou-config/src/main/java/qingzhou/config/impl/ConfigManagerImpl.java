package qingzhou.config.impl;

import qingzhou.framework.ConfigManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class ConfigManagerImpl implements ConfigManager {
    private final File serverXml;
    private final File secureFile;
    private final CipherInternal MASK_CIPHER;

    ConfigManagerImpl(FrameworkContext frameworkContext) {
        File domain = frameworkContext.getFileManager().getDomain();
        serverXml = FileUtil.newFile(domain, "conf", "server.xml");

        try {
            File secureDir = FileUtil.newFile(domain, "data", "secure");
            FileUtil.mkdirs(secureDir);
            secureFile = FileUtil.newFile(secureDir, "secure");
            if (!secureFile.exists()) {
                if (!secureFile.createNewFile()) {
                    throw ExceptionUtil.unexpectedException(secureFile.getPath());
                }
            }
            byte[] defaultBytes = "DONOTCHANGETHISMASK".getBytes(StandardCharsets.UTF_8);
            byte[] tempSec = MessageDigest.getInstance("SHA-256").digest(defaultBytes);
            MASK_CIPHER = new CipherInternal(tempSec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        init();
    }

    private void init() {
//        try { todo
//            String pubKey = getKey(ConfigManager.publicKeyName);
//            String priKey = getKey(ConfigManager.privateKeyName);
//            if (StringUtil.isBlank(pubKey) || StringUtil.isBlank(priKey)) {
//                final String[] pubKey = new String[1];
//                final String[] priKey = new String[1];
//                String secureKey = getKeyOrElseInit(keyFile, keyName, args -> {
//                    Callback<Void, String[]> initKeyPair = initKey;
//                    if (initKeyPair == null) {
//                        initKeyPair = args1 -> new CryptoServiceImpl().generateKeyPair(DEFAULT_INIT_KEY.run(null));
//                    }
//                    String[] keyPair = initKeyPair.run(null);
//                    pubKey[0] = keyPair[0];
//                    priKey[0] = keyPair[1];
//
//                    if (keyName.equals(publicKeyName)) {
//                        return pubKey[0];
//                    } else if (keyName.equals(privateKeyName)) {
//                        return priKey[0];
//                    }
//
//                    return null;
//                });
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public Map<String, String> getConfig(String xpath) {
        return new XmlUtil(serverXml).getAttributes(xpath);
    }

    @Override
    public List<Map<String, String>> getConfigList(String xpath) {
        return new XmlUtil(serverXml).getAttributesList(xpath);
    }

    @Override
    public String getKey(String keyName) throws Exception {
        Properties keyProperties = FileUtil.fileToProperties(secureFile);
        String secVal = keyProperties.getProperty(keyName);

        if (!StringUtil.isBlank(secVal)) {
            secVal = MASK_CIPHER.decrypt(secVal);
        }

        return secVal;
    }

    @Override
    public void writeKey(String keyName, String keyVal) throws Exception {
        if (StringUtil.isBlank(keyVal)) return;

        Properties keyProperties = FileUtil.fileToProperties(secureFile);
        keyVal = MASK_CIPHER.encrypt(keyVal);
        keyProperties.put(keyName, keyVal);
        FileUtil.writeFile(secureFile, StringUtil.propertiesToString(keyProperties));
    }
}
