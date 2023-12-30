package qingzhou.master.security;

import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.EditModel;
import qingzhou.console.impl.ServerXml;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.master.MasterModelBase;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;

import java.util.Map;
import java.util.Objects;


@Model(name = Constants.MODEL_NAME_consolesecurity,
        entryAction = EditModel.ACTION_NAME_EDIT,
        icon = "newspaper-o", menuName = "Security",
        nameI18n = {"控制台安全", "en:Console Security"}, infoI18n = {"配置管理控制台的安全策略。", "en:Configure the security policy of management console."})
public class ConsoleSecurity extends MasterModelBase implements EditModel {

    static {
        ConsoleContext master = ConsoleWarHelper.getMasterAppConsoleContext();
        if (master != null) {
            master.addI18N("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});
        }
    }

    @ModelField(
            isWildcardIp = true,
            skipCharacterCheck = "*",// 其实：有了 isWildcardIp ，已经跳过了 * 了，这里可以不写，但为了易于理解暂时保留了
            nameI18n = {"信任 IP", "en:Trusted IP"},
            infoI18n = {"指定信任的客户端 IP 地址，其值可为具体的 IP、匹配 IP 的正则表达式或通配符 IP（如：168.1.2.*，168.1.4.5-168.1.4.99）。远程的客户端只有在被设置为信任后，才可进行首次默认密码更改、文件上传等敏感操作。注：不设置表示只有 TongWeb 的安装机器受信任，设置为 * 表示信任所有机器（不建议）。", "en:Specifies the trusted client IP address, whose value can be a specific IP or a regular expression that matches an IP, or a wildcard IP (for example: 168.1.2.*, 168.1.4.5-168.1.4.99). Remote clients can only perform sensitive operations such as first default password changes, file uploads, etc. only after they are set to trust. Note: No setting means that only the installation machine of TongWeb is trusted, and setting to * means that all machines are trusted (not recommended)."})
    public String trustedIP;

    @ModelField(
            type = FieldType.bool,
            nameI18n = {"禁用文件上传", "en:Disable File Upload"},
            infoI18n = {"禁用文件上传可以在很大程度上保护系统的安全，禁用后，攻击者将不能再通过控制台来进行部署木马应用、上传恶意程序等操作。禁用文件上传后，您仍然可以选择服务器上已有的文件来进行相关操作。注：出于安全考虑，建议保持禁用文件上传功能。",
                    "en:Disabling file upload can protect the security of the system to a large extent, and after disabling it, attackers will no longer be able to deploy Trojan applications, upload malicious programs, and other operations through the console. When file uploads are disabled, you can still select files already on the server to perform related operations. Note: For security reasons, it is recommended to keep the file upload feature disabled."})
    public boolean disableUpload = true;

    @ModelField(
            type = FieldType.bool,
            nameI18n = {"禁用文件下载", "en:Disable File Download"},
            infoI18n = {"禁用文件下载可以在很大程度上保护系统的安全，禁用后，攻击者将不能再通过控制台来下载系统相关的文件。出于安全考虑，建议保持禁用文件下载功能。",
                    "en:Disabling file downloads can protect the system to a great extent, and once disabled, attackers will no longer be able to download system-related files through the console. For security reasons, it is recommended to keep the file download feature disabled."})
    public boolean disableDownload = true;

    @Override
    public void update(Request request, Response response) throws Exception {
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        boolean written = false;
        String[] ss = {"disableUpload", "disableDownload"};
        for (String s : ss) {
            if (updateDisableParameter(request.getParameter(s), s, xmlUtil)) {
                written = true;
            }
        }

        String trustedIP = request.getParameter("trustedIP");
        if (!Objects.equals(trustedIP, ServerXml.get().trustedIP())) { // 注意 可以是空串，空串表示 清空 信任 IP
            xmlUtil.setAttribute("//root/console", "trustedIP", trustedIP);
            written = true;
        }

        if (written) {
            xmlUtil.write();
        }
    }

    private Boolean updateDisableParameter(String disableParameter, String disableTag, XmlUtil xmlUtil) {
        if (StringUtil.notBlank(disableParameter)) {
            boolean disableParameterInXml = true;
            String data = xmlUtil.getAttributes("//root/console").get(disableTag);
            if (StringUtil.notBlank(data)) {
                disableParameterInXml = Boolean.parseBoolean(data);
            }

            boolean disable = Boolean.parseBoolean(disableParameter);
            if (disable != disableParameterInXml) {
                xmlUtil.setAttribute("//root/console", disableTag, String.valueOf(disable));
                return true;
            }
        }
        return false;
    }

    @Override
    public void show(Request request, Response response) throws Exception {
        ConsoleSecurity consoleSecurity = new ConsoleSecurity();
        consoleSecurity.trustedIP = ServerXml.get().trustedIP();
        consoleSecurity.disableUpload = true;
        XmlUtil xmlUtil = new XmlUtil(ConsoleWarHelper.getServerXml());
        Map<String, String> attributes = xmlUtil.getAttributes("//root/console");
        String disableUpload = attributes.getOrDefault("disableUpload", "true");
        consoleSecurity.disableUpload = Boolean.parseBoolean(disableUpload);
        String disableDownload = attributes.getOrDefault("disableDownload", "true");
        consoleSecurity.disableDownload = Boolean.parseBoolean(disableDownload);
        response.addDataObject(consoleSecurity);
    }

}
