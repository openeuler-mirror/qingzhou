package qingzhou.config;

import qingzhou.engine.ServiceRegister;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;

import java.io.File;

public class Controller extends ServiceRegister<Config> {
    @Override
    public Class<Config> serviceType() {
        return Config.class;
    }

    @Override
    protected Config serviceObject() {
        File qzXml = new File(moduleContext.getInstanceDir(), "qingzhou.xml");
        String type = new XmlUtil(qzXml).getAttributes("//config").get("type");
        // todo: 从 qingzhou.xml 中解析配置的类型，本地文件，或者配置中心
        if ("local".equals(type)) {
            CryptoService cryptoService = moduleContext.getService(CryptoService.class);
            return new LocalXmlConfig(qzXml, cryptoService);
        }
        throw new IllegalArgumentException();
    }
}
