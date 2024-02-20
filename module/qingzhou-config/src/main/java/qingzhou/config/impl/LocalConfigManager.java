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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

class LocalConfigManager implements ConfigManager {
    private File home;
    private File domain;
    private File libDir;

    private final File serverXml;
    private final File secureFile;

    private final KeyCipher MASK_CIPHER;

    LocalConfigManager(FrameworkContext frameworkContext) {

        File domain = getDomain();
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

    @Override
    public String getVersion() {
        String qzVerName = "version";
        return getLib().getName().substring(qzVerName.length());
    }

    @Override
    public File masterApp() {
        return FileUtil.newFile(getLib(), "sysapp", FrameworkContext.SYS_APP_MASTER);
    }

    @Override
    public File commonApp() {
        return FileUtil.newFile(getLib(), "sysapp", "common");
    }

    @Override
    public File appsDir() {
        return FileUtil.newFile(getDomain(), "apps");
    }

    @Override
    public File nodeAgentApp() {
        return FileUtil.newFile(getLib(), "sysapp", FrameworkContext.SYS_APP_NODE_AGENT);
    }

    @Override
    public File consoleApp() {
        return FileUtil.newFile(getLib(), "sysapp", "console");
    }

    private File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
    }

    private File getDomain() {
        if (domain == null) {
            String domainName = System.getProperty("qingzhou.domain");
            if (domainName == null || domainName.trim().isEmpty()) {
                throw new NullPointerException("qingzhou.domain");
            }
            domain = new File(domainName).getAbsoluteFile();
        }
        return domain;
    }

    private File getLib() {
        if (libDir == null) {
            String jarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/version";
            int i = jarPath.lastIndexOf(flag);
            int j = jarPath.indexOf("/", i + flag.length());
            libDir = new File(new File(getHome(), "lib"), jarPath.substring(i + 1, j));
        }
        return libDir;
    }

    @Override
    public File getTemp(String subName) {
        File tmpdir;
        File domain = getDomain();
        if (domain != null) {
            tmpdir = new File(domain, "temp");
        } else {
            tmpdir = new File(System.getProperty("java.io.tmpdir"));
        }
        if (StringUtil.notBlank(subName)) {
            tmpdir = new File(tmpdir, subName);
        }
        FileUtil.mkdirs(tmpdir);
        return tmpdir;
    }
}
