package qingzhou.config;

import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.KeyCipher;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.util.*;

class LocalConfig implements Config {
    private final File serverXml;
    private final File secureFile;

    private final KeyCipher MASK_CIPHER;

    LocalConfig(FrameworkContext frameworkContext, CryptoService cryptoService) {

        File domain = frameworkContext.getDomain();
        serverXml = FileUtil.newFile(domain, "conf", "server.xml");

        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        secureFile = FileUtil.newFile(secureDir, "secure");
        FileUtil.createNewFile(secureFile);

        try {
            MASK_CIPHER = cryptoService.getKeyCipher("-==DONOTCHANGETHISMASK==-");
            initKeys(cryptoService);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void initKeys(CryptoService cryptoService) throws Exception {
        String[] keys = {localKeyName, remoteKeyName};
        for (String key : keys) {
            if (StringUtil.isBlank(getKey(key))) {
                writeKey(key, UUID.randomUUID().toString().replace("-", ""));
            }
        }

        if (StringUtil.isBlank(getKey(Config.publicKeyName))
                || StringUtil.isBlank(getKey(Config.privateKeyName))) {
            String[] keyPair = cryptoService.generateKeyPair(UUID.randomUUID().toString().replace("-", ""));
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
        FileUtil.writeFile(secureFile, propertiesToString(keyProperties));
    }

    public static String propertiesToString(Properties properties) {
        if (properties == null) {
            return "";
        }
        final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        List<String> list = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(System.lineSeparator(), list);
    }
}
