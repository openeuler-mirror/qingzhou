package qingzhou.app.nodeagent.config;


import qingzhou.api.AppContext;
import qingzhou.api.FieldType;
import qingzhou.api.Group;
import qingzhou.api.Groups;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Editable;

@Model(name = "logpush", icon = "upload-alt",
        nameI18n = {"日志推送", "en:Log Push"}, entryAction = Editable.ACTION_NAME_EDIT,
        infoI18n = {"将 TongWeb 的运行日志推送到远端日志存储服务器（如 Elasticsearch）上，以便于统一管理。开启本功能后，TongWeb 的日志除了在本地文件存储之外，还会推送到指定的日志存储服务器。注：当无法连接到日志服务器时，日志推送将会失败，并且目前 TongWeb 不会进行补充推送。",
                "en:Push TongWeb operational logs to remote log storage servers such as Elasticsearch for unified management. After this feature is enabled, TongWeb logs are not only stored in local files, but also pushed to the specified log storage server. Note: When you cannot connect to the log server, the log push will fail, and TongWeb will not perform supplemental push at this time."})
public class LogPush extends ModelBase implements Editable {
    private final String group_basic = "group_basic";
    private final String group_elasticsearch = "group_elasticsearch";

    @Override
    public void init() {
        AppContext appContext = getAppContext();
        appContext.addI18n("logpush.validation.index.error", new String[]{"索引格式有误，索引名只能以小写字母、数字、_、-构成，且必需以小写字母或数字开头", "en:The index format is incorrect, and the index name can only be composed of lowercase letters, numbers, _, -, and must start with lowercase letters or numbers"});
        appContext.addI18n("logpush.validation.pushAddress.invalid", new String[]{"推送地址不可用", "en:Push address not available"});
    }

    @ModelField(
            type = FieldType.bool,
            group = group_basic,
            nameI18n = {"启用", "en:Enable log pushing"},
            infoI18n = {"设置是否要启用 TongWeb 的日志推送功能。",
                    "en:Set whether you want to enable TongWeb log push."})
    public Boolean enabled = false;

    @ModelField(
            required = true,
            group = group_basic,
            isURL = true,
            effectiveWhen = "enabled=true",
            skipCharacterCheck = "[]",
            nameI18n = {"推送地址", "en:Push Address"},
            infoI18n = {"设置连接远程日志服务器的 url（支持的协议头信息为 http:// 或 https://），通常是一个 REST 接口（如 Elasticsearch 提供的 REST 接口）。",
                    "en:Set the URL to connect to the remote log server (supported protocol header information is http:// or https://), typically a REST interface such as the one provided by Elasticsearch."})
    public String pushAddress;

    @ModelField(
            group = group_basic,
            effectiveWhen = "enabled=true",
            nameI18n = {"用户名", "en:User Name"},
            infoI18n = {"指定连接远程日志服务器的身份信息。", "en:Specifies the identity information that connects to the remote log server."}
    )
    public String username;

    @ModelField(
            type = FieldType.password, maxLength = 2048,
            group = group_basic,
            effectiveWhen = "enabled=true",
            nameI18n = {"密码", "en:Authentication Password"},
            infoI18n = {"指定连接远程日志服务器的认证信息。", "en:Specifies the authentication information for connecting to the remote log server."}
    )
    public String password;

    @ModelField(
            required = true,
            notSupportedCharacters = "/",
            group = group_elasticsearch,
            effectiveWhen = "enabled=true",
            nameI18n = {"索引", "en:Index"},
            infoI18n = {"ElasticSearch 服务器存放日志数据的索引。", "en:The index of the ElasticSearch server that holds the log data."}
    )
    public String index;

    @Override
    public Groups groups() {
        return Groups.of(
                Group.of(group_basic, new String[]{"基本属性", "en:Basic"}),
                Group.of(group_elasticsearch, new String[]{"ES 配置", "en:ES Config"})
        );
    }

    @Override
    public String validate(Request request, String fieldName) {
        if ("index".equals(fieldName)) {
            String newValue = request.getParameter(fieldName);
            if (!newValue.matches("[a-z0-9][a-z0-9_-]*")) {
                return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), "logpush.validation.index.error");
            }
        }

        return super.validate(request, fieldName);
    }
}