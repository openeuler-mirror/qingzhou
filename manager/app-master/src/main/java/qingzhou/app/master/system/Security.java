package qingzhou.app.master.system;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Updatable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(code = "security", icon = "shield",
        menu = "System", order = 4, entrance = "edit",
        name = {"平台安全", "en:Security"},
        info = {"配置 轻舟 管理控制台的安全策略。", "en:Configure the security policy of QingZhou management console."})
public class Security extends ModelBase implements Updatable {
    @ModelField(
            required = false,
            name = {"信任 IP", "en:Trusted IP"},
            info = {"指定信任的客户端 IP 地址，其值可为具体的 IP、匹配 IP 的正则表达式或通配符 IP（如：168.1.2.*，168.1.4.5-168.1.4.99）。远程的客户端只有在被设置为信任后，才可进行首次默认密码更改、文件上传等敏感操作。注：不设置表示只有 TongWeb 的安装机器受信任，设置为 * 表示信任所有机器（不建议）。", "en:Specifies the trusted client IP address, whose value can be a specific IP or a regular expression that matches an IP, or a wildcard IP (for example: 168.1.2.*, 168.1.4.5-168.1.4.99). Remote clients can only perform sensitive operations such as first default password changes, file uploads, etc. only after they are set to trust. Note: No setting means that only the installation machine of TongWeb is trusted, and setting to * means that all machines are trusted (not recommended)."})
    public String trustedIP;

    @ModelField(
            type = FieldType.number, required = false, min = 1,
            name = {"失败锁定次数", "en:User Lockout Threshold"},
            info = {"用户连续认证失败后被锁定的次数。", "en:The number of times a user has been locked out after successive failed authentication attempts."})
    public int failureCount = 5;

    @ModelField(
            type = FieldType.number, required = false, min = 60,
            name = {"锁定时长", "en:Lock Duration"},
            info = {"用户在多次认证失败后被锁定的时间（秒）。",
                    "en:The amount of time (in seconds) that a user is locked out after multiple authentication failures."})
    public int lockOutTime = 300;

    @ModelField(
            required = false, type = FieldType.bool,
            name = {"启用验证码", "en:Enable Verification Code"},
            info = {"开启用户登录时的验证码校验，当首次登录失败后，再次登录需要输入验证码。", "en:Enable authentication code verification during user login, when the first login fails, you need to enter the authentication code to login again."})
    public boolean verCodeEnabled;

    @ModelField(
            required = false, type = FieldType.number, min = 0, max = 90,
            name = {"密码最长使用期限", "en:Maximum Password Age"},
            info = {"用户登录系统的密码距离上次修改超过该期限（单位为天）后，需首先更新密码才能继续登录系统。",// 内部：0 表示可以永久不更新。
                    "en:After the password of the user logging in to the system has been last modified beyond this period (in days), the user must first update the password before continuing to log in to the system."})
    public Integer passwordMaxAge = 0;

    @ModelField(
            required = false, type = FieldType.number, min = 1, max = 10,
            name = {"不与最近密码重复", "en:Recent Password Restrictions"},
            info = {"限制本次更新的密码不能和最近几次使用过的密码重复。注：设置为 “1” 表示只要不与当前密码重复即可。",
                    "en:Restrict this update password to not be duplicated by the last few times you have used. Note: A setting of 1 means as long as it does not duplicate the current password."})
    public Integer passwordLimitRepeats = 1;

    @ModelField(
            required = false, type = FieldType.textarea,
            editable = false, createable = false,
            lengthMax = 1000,
            name = {"加密公钥", "en:Public Key"},
            info = {"为了能够管理远端的实例，需要将此密钥在远端的实例进行保存。此外，客户端通过 REST、JMX 等接口管理轻舟实例时，也需要使用此密钥对敏感数据进行加密后再传输。",
                    "en:In order to manage the remote instance, you need to save the key in the remote instance. In addition, when clients manage QingZhou instances through interfaces such as REST and JMX, they also need to use this key to encrypt sensitive data before transmission."})
    public String publicKey;

    private static class SecurityDataStore implements DataStore {
        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            qingzhou.config.Security security = MasterApp.getService(Config.class).getConsole().getSecurity();
            Map<String, String> propertiesFromObj = Utils.getPropertiesFromObj(security);
            List<Map<String, String>> list = new ArrayList<>();
            list.add(propertiesFromObj);
            return list;
        }

        @Override
        public void addData(String id, Map<String, String> data) throws Exception {
            throw new RuntimeException("No Support.");
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.Security security = config.getConsole().getSecurity();
            Utils.setPropertiesToObj(security, data);
            config.setSecurity(security);
        }

        @Override
        public void deleteDataById(String id) {
            throw new RuntimeException("No Support.");
        }
    }
}
