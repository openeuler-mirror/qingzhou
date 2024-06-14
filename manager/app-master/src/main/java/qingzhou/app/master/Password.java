package qingzhou.app.master;

import qingzhou.api.DataStore;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Editable;
import qingzhou.app.master.system.User;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.MessageDigest;
import qingzhou.registry.AppInfo;

import java.util.HashMap;
import java.util.Map;

@Model(code = "password", icon = "key",
        order = 99,
        hidden = true,
        entrance = Editable.ACTION_NAME_EDIT,
        name = {"修改密码", "en:Change Password"},
        info = {"用于修改当前登录用户的密码。", "en:Used to change the password of the currently logged-in user."})
public class Password extends ModelBase implements Editable {
    public static String[] splitPwd(String storedCredentials) {
        String SP = "$";
        String[] pwdArray = new String[4];
        int lastIndexOf = storedCredentials.lastIndexOf(SP);

        String digestAlg = storedCredentials.substring(lastIndexOf + 1);
        pwdArray[pwdArray.length - 1] = digestAlg;

        storedCredentials = storedCredentials.substring(0, lastIndexOf);
        String[] oldPwdDigestStyle = storedCredentials.split("\\" + SP);

        oldPwdDigestStyle[1] = String.valueOf(oldPwdDigestStyle[1].length() / 2);
        System.arraycopy(oldPwdDigestStyle, 0, pwdArray, 0, pwdArray.length - 1);

        return pwdArray;
    }

    @ModelField(
            type = FieldType.password,
            name = {"原始密码", "en:Original Password"},
            info = {"登录系统的原始密码。", "en:The original password to log in to the system."})
    public String originalPassword;

    @ModelField(
            type = FieldType.password,
            name = {"新密码", "en:Password"},
            info = {"用于登录系统的新密码。", "en:The new password used to log in to the system."})
    public String newPassword;

    @ModelField(
            type = FieldType.password,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认用于登录系统的新密码。", "en:Confirm the new password used to log in to the system."})
    public String confirmPassword;

   /* @ModelField(
            required = false,
            type = FieldType.bool,
            name = {"更新双因子认证密钥", "en:Update Two-factor Authentication Key"},
            info = {"安全起见，建议定期刷新双因子认证密钥。刷新后，需要重新在用户终端的双因子认证客户端设备进行绑定。",
                    "en:For security reasons, it is recommended to periodically refresh the two-factor authentication key. After refreshing, you need to re-bind it on the two-factor authentication client device of the user terminal."}
    )
    public boolean update2FA = false;*/

    @Override
    public void start() {
        appContext.addI18n("update2FA.rebind", new String[]{"双因子认证密钥更新成功，请扫描二维码重新绑定双因子认证密钥",
                "en:The two-factor authentication key is updated successfully, please scan the QR code to re-bind the two-factor authentication key"});
        appContext.addI18n("password.confirm.failed", new String[]{"确认密码与新密码不一致", "en:Confirm that the password does not match the new password"});
        appContext.addI18n("password.original.failed", new String[]{"原始密码错误", "en:The original password is wrong"});
        appContext.addI18n("password.change.not", new String[]{"新密码与原始密码是一样的，没有发生改变", "en:The new password is the same as the original password and has not changed"});
        appContext.addI18n("password.doNotUseOldPasswords", new String[]{"出于安全考虑，禁止使用最近 %s 次使用过的旧密码",
                "en:For security reasons, the use of old passwords that have been used last %s is prohibited"});
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        response.addModelData(new Password());
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    public void show(Request request, Response response) {
    }

    private Map<String, String> prepareParameters(Request request) {
        AppInfo master = MasterApp.getService(Deployer.class).getApp(DeployerConstants.MASTER_APP_NAME).getAppInfo();
        Map<String, String> properties = new HashMap<>();
        for (String fieldName : master.getModelInfo(request.getModel()).getFormFieldNames()) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新密码。", "en:Update the password."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> p = new HashMap<>();
        Map<String, String> paramMap = prepareParameters(request);
        String validate;
        for (String name : paramMap.keySet()) {
            validate = validate(request, name);
            if (validate != null) {
                response.setSuccess(false);
                response.setMsg(validate);
                return;
            }
        }

        Console console = MasterApp.getService(Config.class).getConsole();
        String loginUser = request.getUser();
        if (Boolean.parseBoolean(p.getOrDefault("update2FA", "false"))) {
            // p.put("keyFor2FA", User.refresh2FA()); TODO
            p.put("bound2FA", "false");
        } else {
            if (forbidResetInitPwd(request)) {
                response.setSuccess(false);
                return;
            }

            Map<String, String> loginUserPro = getDataStore().getDataById(loginUser);

            MessageDigest digest = CryptoServiceFactory.getInstance().getMessageDigest();

            String[] passwords = splitPwd(loginUserPro.get("password"));
            String digestAlg = passwords[0];
            int saltLength = Integer.parseInt(passwords[1]);
            int iterations = Integer.parseInt(passwords[2]);

            p.put(User.pwdKey,
                    digest.digest(
                            paramMap.get("newPassword"),
                            digestAlg,
                            saltLength,
                            iterations));
            p.put("changeInitPwd", "false");

            User.insertPasswordModifiedTime(p);

            String historyPasswords = User.cutOldPasswords(
                    loginUserPro.remove("historyPasswords"),
                    console.getSecurity().getPasswordLimitRepeats(),
                    p.get(User.pwdKey));
            p.put("historyPasswords", historyPasswords);
        }

        getDataStore().updateDataById(loginUser, p);
        qingzhou.config.User user = console.getUser(loginUser);
        user.setPassword(p.get(User.pwdKey));
        user.setChangeInitPwd(Boolean.parseBoolean(p.get("changeInitPwd")));

        if (Boolean.parseBoolean(paramMap.getOrDefault("update2FA", "false"))) {
            response.setMsg(appContext.getI18n(request.getLang(), "update2FA.rebind"));
        } else {
            // 注销在其它机器上忘记注销的会话
//            String encodeUser = LoginManager.encodeUser(actionContext.getId());
//            ActionContext.invalidateAllSessionAsAttribute(actionContext.getHttpServletRequestInternal(),
//                    LoginManager.LOGIN_USER, encodeUser);
//
//            actionContext.invalidate();// 注销当前会话，强制自己重新登录
        }
    }


    public String validate(Request request, String fieldName) throws Exception {
        /*if (!ServerXml.get().authEnabled()) {
            return null;
        }*/

        String loginUser = request.getUser();
        String update2FA = request.getParameter("update2FA");
        String newValue;
        if (null == update2FA || update2FA.isEmpty() || !Boolean.parseBoolean(update2FA)) {
            if ("originalPassword".equals(fieldName)) {
                newValue = request.getParameter("originalPassword");
                if (newValue == null) { // fix #ITAIT-2849
                    return appContext.getI18n(request.getLang(), "validator.require");
                }

                String pwd = getDataStore().getDataById(loginUser).get(User.pwdKey);

                MessageDigest digest = CryptoServiceFactory.getInstance().getMessageDigest();
                if (!digest.matches(newValue, pwd)) {
                    return appContext.getI18n(request.getLang(), "password.original.failed");
                }
            }

            if ("newPassword".equals(fieldName)) {
                newValue = request.getParameter("newPassword");
                if (newValue == null) { // fix #ITAIT-2849
                    return appContext.getI18n(request.getLang(), "validator.require");
                }

                String msg = User.checkPwd(appContext, request.getLang(), newValue, loginUser);
                if (msg != null) {
                    return msg;
                }

                String pwd = getDataStore().getDataById(loginUser).get(User.pwdKey);
                MessageDigest digest = CryptoServiceFactory.getInstance().getMessageDigest();
                boolean matches = digest.matches(newValue, pwd);
                if (matches) {
                    return appContext.getI18n(request.getLang(), "password.change.not");
                }

                Map<String, String> loginUserPro = getDataStore().getDataById(loginUser);
                if (loginUserPro != null) {
                    String historyPasswords = loginUserPro.get("historyPasswords");
                    if (null != historyPasswords && !historyPasswords.isEmpty()) {
                        for (String oldPass : historyPasswords.split(",")) {
                            if (!oldPass.isEmpty() && digest.matches(newValue, oldPass)) {
                                int limitRepeats = MasterApp.getService(Config.class).getConsole().getSecurity().getPasswordLimitRepeats();
                                return String.format(appContext.getI18n(request.getLang(), "password.doNotUseOldPasswords"), limitRepeats);
                            }
                        }
                    }
                }
            }

            if ("confirmPassword".equals(fieldName)) {
                newValue = request.getParameter("confirmPassword");
                if (newValue == null) { // fix #ITAIT-2849
                    return appContext.getI18n(request.getLang(), "validator.require");
                }
                if (!newValue.equals(request.getParameter("newPassword"))) {
                    return appContext.getI18n(request.getLang(), "password.confirm.failed");
                }
            }
        }

        return null;
    }

    private boolean forbidResetInitPwd(Request request) throws Exception {
        String loginUser = request.getUser();
        if (!"qingzhou".equals(loginUser)) { // 非三员或特殊用户不必限制
            return false;
        }
        return false;
    }

    @ModelAction(icon = "shield", name = {"二维码", "en:QR Code"},
            info = {"获取当前用户的双因子认证密钥，以二维码形式提供给用户。", "en:Obtain the current user two-factor authentication key and provide it to the user in the form of a QR code."})
    public void showKeyFor2FA(Request request, Response response) throws Exception {
        /*String loginUser = request.getLoginUser();
        Map<String,String> attributes = ConsoleXml.get().user(loginUser);
        String key = "keyFor2FA";
        String keyFor2FA = attributes.get(key);
        if (StringUtil.isBlank(keyFor2FA)) {
            //keyFor2FA = User.refresh2FA();
            User.updateXmlProperty(loginUser, key, keyFor2FA);
        }
        String qrCode = Totp.generateTotpString(loginUser, keyFor2FA);

        request.setResponseHeader("Pragma", "no-cache");
        request.setResponseHeader("Cache-Control", "no-cache");
        String format = "PNG";
        request.setResponseContentType("image/" + format);
        // 请求来自内部命令行调用
        if (StringUtil.notBlank(request.getHeader(LoginManager.HEADER_CLIENT_TYPE))) {
            request.setResponseHeader("Content-disposition", "attachment; filename=" + key + ".png");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TWQRCodeConfig twqrCodeConfig = new TWQRCodeConfig();
        twqrCodeConfig.setText(qrCode)
                .setWidth(300)
                .setHeight(300)
                .setFormat(format)
                .setOutputStream(bos);
        QRCodeGenerator.generateImage(twqrCodeConfig);

        // 二维码返回到浏览器
        request.getResponseOutputStream().write(bos.toByteArray());
        request.getResponseOutputStream().flush();*/
    }

    @ModelAction(
            name = {"双因子密码验证", "en:Validate 2FA Code"},
            info = {"验证双因子密码是否正确。", "en:Verify that the two-factor password is correct."})
    public void validate(Request request, Response response) throws Exception {
        boolean result = false;
        String reqCode = request.getParameter("password2fa");
        if (null != reqCode && !reqCode.isEmpty()) {
            String loginUser = request.getUser();
            Map<String, String> attributes = getDataStore().getDataById(loginUser);
            String keyFor2FA = attributes.get("keyFor2FA");
            try {
                //result = Totp.verify(keyFor2FA, reqCode); TODO
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (result) {
                attributes.put("bound2FA", "true");
                getDataStore().updateDataById(loginUser, attributes);
            }
        }
        //response.setDirectBody("{\"result\":" + result + "}");
    }

    @Override
    public DataStore getDataStore() {
        return User.USER_DATA_STORE;
    }
}
