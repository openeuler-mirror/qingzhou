package qingzhou.master.service;

import qingzhou.framework.api.*;
import qingzhou.master.MasterModelBase;

@Model(name = "centralizedconfig", icon = "file-text-o",
        menuName = "Service", menuOrder = 5,
        entryAction = ShowModel.ACTION_NAME_SHOW,
        nameI18n = {"集中配置", "en:Centralized Config"},
        infoI18n = {"对集中管理过程中环境、安全等方面进行配置。",
                "en:Configure the environment and security in the centralized management process."})
public class CentralizedConfig extends MasterModelBase {
    @ModelField(nameI18n = {"加密公钥", "en:Public Key"},
            infoI18n = {"为了能够管理远端的 TongWeb 节点或实例，需要将此密钥在远端的 TongWeb 节点或实例进行保存（在其“全局配置”模块打开“支持集中管理”后设置）。此外，客户端通过 REST、JMX 等接口管理 TongWeb 服务器时，也需要使用此密钥对敏感数据进行加密后再传输。",
                    "en:In order to be able to manage the remote TongWeb node or instance, you need to save this key in the remote TongWeb node or instance (set after its \"Global Configuration\" module turns on \"Support centralized management\"). In addition, when the client manages the TongWeb server through interfaces such as REST and JMX, it also needs to use this key to encrypt sensitive data before transmission."})
    public String publicKey;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "info",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    public void show(Request request, Response response) throws Exception {
        // todo
//        String secureKeyAsKeyPair = SecureKey.getOrInitKeyPair(getAppContext().getDomain(), SecureKey.publicKeyName, SecureKey.publicKeyName, SecureKey.privateKeyName);
//        Map<String, String> map = new HashMap<>();
//        map.put("publicKey", secureKeyAsKeyPair);
//        response.addData(map);
    }
}
