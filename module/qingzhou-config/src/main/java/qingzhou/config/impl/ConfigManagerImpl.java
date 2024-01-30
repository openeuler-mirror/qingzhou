package qingzhou.config.impl;

import qingzhou.framework.ConfigManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.InternalService;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ConfigManagerImpl implements ConfigManager, InternalService {
    private final File serverXml;

    public ConfigManagerImpl(FrameworkContext frameworkContext) {
        serverXml = FileUtil.newFile(frameworkContext.getFileManager().getDomain(), "conf", "server.xml");
    }

    @Override
    public Map<String, String> getConfig(String xpath) {
        return new XmlUtil(serverXml).getAttributes(xpath);
    }

    @Override
    public List<Map<String, String>> getConfigList(String xpath) {
        return new XmlUtil(serverXml).getAttributesList(xpath);
    }
}
