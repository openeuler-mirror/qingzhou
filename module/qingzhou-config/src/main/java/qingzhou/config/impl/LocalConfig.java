package qingzhou.config.impl;

import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.framework.Framework;
import qingzhou.framework.pattern.Callback;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.util.*;

class LocalConfig implements Config {
    private final File serverXml;
    private final File secureFile;

    private final KeyCipher MASK_CIPHER;

    LocalConfig(Framework framework, CryptoService cryptoService) {

        File domain = framework.getDomain();
        serverXml = FileUtil.newFile(domain, "conf", "server.xml");

        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        secureFile = FileUtil.newFile(secureDir, "secure");
        FileUtil.createNewFile(secureFile);

        try {
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

        if (StringUtil.isBlank(getKey(Config.publicKeyName))
                || StringUtil.isBlank(getKey(Config.privateKeyName))) {
            String[] keyPair = cryptoService.generateKeyPair(DEFAULT_INIT_KEY.run(null));
            writeKey(Config.publicKeyName, keyPair[0]);
            writeKey(Config.privateKeyName, keyPair[1]);
        }
    }

    @Override
    public boolean existsConfig(String index) {
        return new XmlUtil(serverXml).isNodeExists(index);
    }

    @Override
    public Map<String, String> getConfig(String xpath) {
        return new XmlUtil(serverXml).getAttributes(xpath);
    }

    @Override
    public void updateConfig(String index, Map<String, String> config) {
        XmlUtil xmlUtil = new XmlUtil(serverXml);
        xmlUtil.setAttributes(index, config);
        xmlUtil.write();
    }

    @Override
    public List<Map<String, String>> getConfigList(String xpath) {
        List<Map<String, String>> list = new XmlUtil(serverXml).getAttributesList(xpath);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    public void addConfig(String parentIndex, String flag, Map<String, String> properties) {
        XmlUtil xmlUtil = new XmlUtil(serverXml);
        xmlUtil.addNew(parentIndex, flag, properties);
        xmlUtil.write();
    }

    @Override
    public void deleteConfig(String index) {
        XmlUtil xmlUtil = new XmlUtil(serverXml);
        xmlUtil.delete(index);
        xmlUtil.write();
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
