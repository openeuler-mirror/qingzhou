package qingzhou.app.master.service;

import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Option;
import qingzhou.framework.api.Options;

// todo：ConfigManager 提供对应的实现在中心上存取数据，取代 server.xml 和 secure 文件。
@Model(name = "registry", icon = "cloud-upload",
        nameI18n = {"注册中心", "en:Registry Server"},
        infoI18n = {"注册中心是一种外置的存储系统，用于实现轻舟平台的配置共享和服务发现。",
                "en:The registry is an external storage system that is used to realize configuration sharing and service discovery of the Qingzhou platform."})
public class Registry extends ModelBase implements AddModel {
    @ModelField(
            showToList = true,
            required = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"注册中心的唯一标识。", "en:A unique identifier for the registry server."})
    private String id;

    @ModelField(
            showToList = true,
            type = FieldType.radio,
            required = true,
            nameI18n = {"服务提供商", "en:Service Vendor"},
            infoI18n = {"选择提供该注册中心服务的提供商。", "en:Select the provider that provides the registry service."})
    private String registryServerType;

    @ModelField(
            showToList = true,
            required = true,
            notSupportedStrings = {"http://", "https://", "dict://", "file://", "gopher://"},
            notSupportedCharacters = "?@[]",
            nameI18n = {"服务地址", "en:Server Address"},
            infoI18n = {"指定服务器的连接地址。", "en:Specifies the connection address of the server."}
    )
    private String address;

    @ModelField(
            required = true,
            nameI18n = {"用户名", "en:Username"},
            infoI18n = {"连接服务器所需要的用户名。", "en:The user name required to connect to the server."}
    )
    private String username;

    @ModelField(
            type = FieldType.password, maxLength = 2048,
            required = true,
            nameI18n = {"密码", "en:Authentication Password"},
            infoI18n = {"连接服务器所需要的认证密码。", "en:The authentication password required to connect to the server."}
    )
    private String password;

    @ModelField(
            showToList = true,
            required = true,
            nameI18n = {"标识符", "en:Identifier"},
            infoI18n = {"在外置的存储系统上，区别于其它使用者的标识符。",
                    "en:On an external storage system, an identifier that distinguishes it from other users."})
    private String registryName;

    @Override
    public Options options(String fieldName) {
        if ("registryServerType".equals(fieldName)) {
            return Options.of(Option.of("etcd"), Option.of("Nacos"));
        }
        return null;
    }
}
