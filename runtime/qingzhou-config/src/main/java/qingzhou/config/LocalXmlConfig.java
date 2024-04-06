package qingzhou.config;

import qingzhou.engine.ModuleContext;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.crypto.KeyCipher;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.util.*;

class LocalXmlConfig implements Config {
    private final File qzXml;
    private final KeyCipher MASK_CIPHER;

    LocalXmlConfig(File qzXml,CryptoService cryptoService) {
        this.qzXml=qzXml;
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
    public Map<String, String> getConfig(String xpath) {
        return new XmlUtil(qzXml).getAttributes(xpath);
    }

    @Override
    public void updateConfig(String index, Map<String, String> config) {
        XmlUtil xmlUtil = new XmlUtil(qzXml);
        xmlUtil.setAttributes(index, config);
        xmlUtil.write();
    }

    @Override
    public List<Map<String, String>> getConfigList(String xpath) {
        List<Map<String, String>> list = new XmlUtil(qzXml).getAttributesList(xpath);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    public void addConfig(String parentIndex, String flag, Map<String, String> properties) {
        XmlUtil xmlUtil = new XmlUtil(qzXml);
        xmlUtil.addNew(parentIndex, flag, properties);
        xmlUtil.write();
    }

    @Override
    public void deleteConfig(String index) {
        XmlUtil xmlUtil = new XmlUtil(qzXml);
        xmlUtil.delete(index);
        xmlUtil.write();
    }

    @Override
    public String getKey(String keyName) throws Exception {
        XmlUtil xmlUtil = new XmlUtil(qzXml);
        Map<String, String> keyProperties = xmlUtil.getAttributes("//secure");
        String secVal = keyProperties.get(keyName);

        if (StringUtil.notBlank(secVal)) {
            secVal = MASK_CIPHER.decrypt(secVal);
        }

        return secVal;
    }

    @Override
    public void writeKey(String keyName, String keyVal) throws Exception {
        if (StringUtil.isBlank(keyVal)) return;

        String finalKeyVal = MASK_CIPHER.encrypt(keyVal);
        XmlUtil xmlUtil = new XmlUtil(qzXml);
        Map<String, String> map = new HashMap<>();
        map.put(keyName, finalKeyVal);
        xmlUtil.setAttributes("//secure", map);
        xmlUtil.write();
    }
}
