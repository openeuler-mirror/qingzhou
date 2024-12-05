package qingzhou.app.system.system;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Update;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.config.Config;

import java.util.Map;

@Model(code = "oauth2", icon = "shield",
        menu = Main.Setting, order = "6",
        entrance = Update.ACTION_EDIT,
        name = {"OAuth2", "en:OAuth2"},
        info = {"配置轻舟以支持OAuth2协议登录。", "en:Configure Qingzhou to support OAuth2 protocol login."})
public class OAuth2 extends ModelBase implements Update {
    @ModelField(
            input_type = InputType.bool,
            name = {"启用 OAuth2 认证", "en:Enable OAuth2 Authentication"},
            info = {"开启 OAuth2 认证，这将会关闭 QingZhou 自身的认证模式，转而切换到指定的 OAuth2 认证模式。",
                    "en:Enabling OAuth2 authentication will turn off QingZhou own authentication mode and switch to the specified OAuth2 authentication mode."}
    )
    public boolean enabled = false;

    @ModelField(
            display = "enabled=true",
            required = true,
            name = {"客户端ID", "en:Client ID"},
            info = {"设置从 OAuth2 服务器注册得到的身份ID。", "en:Set the identity ID registered from the OAuth2 server."})
    public String client_id;

    @ModelField(
            display = "enabled=true",
            required = true,
            input_type = InputType.password,
            name = {"客户端密码", "en:Client Password"},
            info = {"设置从 OAuth2 服务器注册得到的身份ID的验证密码。", "en:Set the authentication password for the ID registered from the OAuth2 server."})
    public String client_secret;

    @ModelField(
            display = "enabled=true",
            required = true,
            name = {"授权地址", "en:Authorized Address"},
            info = {"设置从 OAuth2 服务器获得用户授权的 url 地址。", "en:Set the url address for user authorization from the OAuth2 server."})
    public String authorize_uri;

    @ModelField(
            display = "enabled=true",
            required = true,
            name = {"获取 Token 地址", "en:Get Token Address"},
            info = {"设置从 OAuth2 服务器获取访问 Token 的 url 地址。", "en:Set the url to obtain access to the Token from the OAuth2 server."})
    public String token_uri;

    @ModelField(
            display = "enabled=true",
            required = true,
            name = {"获取用户信息地址", "en:Get User Info Address"},
            info = {"设置从 OAuth2 服务器获取用户账号等信息的 url 地址。", "en:Set the url to obtain information such as user accounts from the OAuth2 server."})
    public String user_uri;

    @Override
    public Map<String, String> editData(String id) {
        qingzhou.config.OAuth2 oAuth2 = Main.getService(Config.class).getCore().getConsole().getOAuth2();
        return ModelUtil.getPropertiesFromObj(oAuth2);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        qingzhou.config.OAuth2 oAuth2 = config.getCore().getConsole().getOAuth2();
        ModelUtil.setPropertiesToObj(oAuth2, data);
        config.setOAuth2(oAuth2);
    }
}
