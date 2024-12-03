package qingzhou.app.system.system;

import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.Group;
import qingzhou.api.type.Update;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.config.Config;

@Model(code = "security", icon = "shield",
        menu = Main.Setting, order = "3",
        entrance = Update.ACTION_EDIT,
        name = {"安全", "en:Security"},
        info = {"配置轻舟管理控制台的安全策略。", "en:Configure the security policy of Qingzhou management console."})
public class Security extends ModelBase implements Update, Group {

    public static final String BASIC = "basic";
    public static final String OAUTH2 = "OAuth2";
    @ModelField(
            group = BASIC,
            name = {"信任 IP", "en:Trusted IP"},
            info = {"指定信任的客户端 IP 地址，其值可为具体的 IP、匹配 IP 的正则表达式或通配符 IP（如：168.1.2.*，168.1.4.5-168.1.4.99）。远程的客户端只有在被设置为信任后，才可进行首次默认密码更改、文件上传等敏感操作。注：不设置表示只有安装机器受信任，设置为 * 表示信任所有机器。",
                    "en:Specifies a trusted client IP address, which can be a specific IP, a regular expression that matches the IP, or a wildcard IP (e.g., 168.1.2.*, 168.1.4.5-168.1.4.99). Only after the remote client is set to trust can it perform sensitive operations such as changing the default password for the first time and uploading files. Note: No set means only the installation machine is trusted, and * means all machines are trusted."})
    public String trustedIp;

    @ModelField(
            group = BASIC,
            input_type = InputType.number,
            min = 1,
            name = {"失败锁定次数", "en:User Lockout Threshold"},
            info = {"用户连续认证失败后被锁定的次数。", "en:The number of times a user has been locked out after successive failed authentication attempts."})
    public Integer failureCount = 5;

    @ModelField(
            group = BASIC,
            input_type = InputType.number,
            min = 60,
            name = {"锁定时长", "en:Lock Duration"},
            info = {"用户在多次认证失败后被锁定的时间（秒）。",
                    "en:The amount of time (in seconds) that a user is locked out after multiple authentication failures."})
    public Integer lockOutTime = 300;

    @ModelField(
            group = BASIC,
            input_type = InputType.bool,
            name = {"启用验证码", "en:Enable Verification Code"},
            info = {"开启用户登录时的验证码校验，当首次登录失败后，再次登录需要输入验证码。", "en:Enable authentication code verification during user login, when the first login fails, you need to enter the authentication code to login again."})
    public Boolean verCodeEnabled = true;

    @ModelField(
            group = BASIC,
            input_type = InputType.number,
            min = 0, max = 90,
            name = {"密码最长使用期限", "en:Maximum Password Age"},
            info = {"用户登录系统的密码距离上次修改超过该期限（单位为天）后，需首先更新密码才能继续登录系统。",// 内部：0 表示可以永久不更新。
                    "en:After the password of the user logging in to the system has been last modified beyond this period (in days), the user must first update the password before continuing to log in to the system."})
    public Integer passwordMaxAge = 0;

    @ModelField(
            group = BASIC,
            input_type = InputType.number,
            min = 1, max = 10,
            name = {"不使用最近密码", "en:Recent Password Restrictions"},
            info = {"限制本次更新的密码不能和最近几次使用过的密码重复。注：设置为 “1” 表示只要不与当前密码重复即可。",
                    "en:Restrict this update password to not be duplicated by the last few times you have used. Note: A setting of 1 means as long as it does not duplicate the current password."})
    public Integer passwordLimitRepeats = 1;

    @ModelField(
            group = BASIC,
            input_type = InputType.textarea,
            edit = false,
            max_length = 1000,
            name = {"加密公钥", "en:Public Key"},
            info = {"为了能够管理远端的实例，需要将此密钥在远端的实例进行保存。此外，客户端通过 REST、JMX 等接口管理轻舟实例时，也需要使用此密钥对敏感数据进行加密后再传输。",
                    "en:In order to manage the remote instance, you need to save the key in the remote instance. In addition, when clients manage Qingzhou instances through interfaces such as REST and JMX, they also need to use this key to encrypt sensitive data before transmission."})
    public String publicKey;

    @ModelField(
            group = OAUTH2,
            input_type = InputType.bool,
            name = {"启用 OAuth2 认证", "en:Enable OAuth2 Authentication"},
            info = {"开启 OAuth2 认证，这将会关闭 QingZhou 自身的认证模式，转而切换到指定的 OAuth2 认证模式。",
                    "en:Enabling OAuth2 authentication will turn off QingZhou own authentication mode and switch to the specified OAuth2 authentication mode."}
    )
    public boolean enabledOAuth2 = false;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            name = {"此服务对外域名", "en:QingZhou Host Address"},
            info = {"此 QingZhou 服务控制台对外可见的域名，注意不要包含其“访问前缀”，例如：http://localhost:9000/。", "en:The url address of the QingZhou service console is visible to the outside world."})
    public String redirectUrl;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            name = {"客户端ID", "en:Client ID"},
            info = {"设置从 OAuth2 服务器注册得到的身份ID。", "en:Set the identity ID registered from the OAuth2 server."})
    public String clientId;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            input_type = InputType.password,
            name = {"客户端密码", "en:Client Password"},
            info = {"设置从 OAuth2 服务器注册得到的身份ID的验证密码。", "en:Set the authentication password for the ID registered from the OAuth2 server."})
    public String clientSecret;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            name = {"授权地址", "en:Authorized Address"},
            info = {"设置从 OAuth2 服务器获得用户授权的 url 地址。", "en:Set the url address for user authorization from the OAuth2 server."})
    public String authorizeUrl;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            name = {"获取 Token 地址", "en:Get Token Address"},
            info = {"设置从 OAuth2 服务器获取访问 Token 的 url 地址。", "en:Set the url to obtain access to the Token from the OAuth2 server."})
    public String tokenUrl;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            name = {"验证 Token 地址", "en:Verify Token Address"},
            info = {"设置从 OAuth2 服务器校验 Token 的 url 地址。", "en:Set the url to verify the Token from the OAuth2 server."})
    public String checkTokenUrl;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            required = true,
            name = {"获取用户信息地址", "en:Get User Info Address"},
            info = {"设置从 OAuth2 服务器获取用户账号等信息的 url 地址。", "en:Set the url to obtain information such as user accounts from the OAuth2 server."})
    public String userInfoUrl;

    @ModelField(
            group = OAUTH2,
            display = "enabledOAuth2=true",
            name = {"注销 Token 地址", "en:Deregistered Token Address"},
            info = {"设置从 OAuth2 服务器注销 Token 的 url 地址。", "en:Set the url for logging off the Token from the OAuth2 server."})
    public String logoutUrl;

    @Override
    public Map<String, String> editData(String id) {
        qingzhou.config.Security security = Main.getService(Config.class).getCore().getConsole().getSecurity();
        return ModelUtil.getPropertiesFromObj(security);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        qingzhou.config.Security security = config.getCore().getConsole().getSecurity();
        ModelUtil.setPropertiesToObj(security, data);
        config.setSecurity(security);
    }

    @Override
    public Item[] groupData() {
        return new Item[]{
                Item.of("basic", new String[]{"基础属性", "en:Basic"}),
                Item.of(OAUTH2, new String[]{"OAuth2", "en:OAuth2"})
        };
    }

}
