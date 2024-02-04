package qingzhou.config.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.framework.ConfigManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.pattern.Callback;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

class ConfigManagerImpl implements ConfigManager {
    private final File serverXml;
    private final File secureFile;
    private final KeyCipher MASK_CIPHER;

    ConfigManagerImpl(FrameworkContext frameworkContext) {

        File domain = frameworkContext.getFileManager().getDomain();
        serverXml = FileUtil.newFile(domain, "conf", "server.xml");

        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        secureFile = FileUtil.newFile(secureDir, "secure");
        FileUtil.createNewFile(secureFile);

        try {
            CryptoService cryptoService = frameworkContext.getServiceManager().getService(CryptoService.class);
            MASK_CIPHER = cryptoService.getKeyCipher("-==DONOTCHANGETHISMASK==-");
            initKeys(cryptoService);
        } catch (Exception e) {
            throw ExceptionUtil.unexpectedException(e);
        }
    }

    private void initKeys(CryptoService cryptoService) throws Exception {
        Callback<Void, String> DEFAULT_INIT_KEY = args -> UUID.randomUUID().toString().replace("-", "");

        String[] keys = {localKeyName, remoteKeyName};
        for (String key : keys) {
            if (StringUtil.isBlank(getKey(key))) {
                writeKey(key, DEFAULT_INIT_KEY.run(null));
            }
        }

        if (StringUtil.isBlank(getKey(ConfigManager.publicKeyName))
                || StringUtil.isBlank(getKey(ConfigManager.privateKeyName))) {
            String[] keyPair = cryptoService.generateKeyPair(DEFAULT_INIT_KEY.run(null));
            writeKey(ConfigManager.publicKeyName, keyPair[0]);
            writeKey(ConfigManager.privateKeyName, keyPair[1]);
        }
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
