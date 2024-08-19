package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.console.Totp;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.engine.util.Utils;
import qingzhou.qr.QrGenerator;

import java.util.HashMap;
import java.util.Map;

@Model(code = "password", icon = "key",
        menu = "System", order = 3, entrance = "edit",
        name = {"密码管理", "en:Password"},
        info = {"用于修改当前登录用户的密码、动态密码等。",
                "en:It is used to change the password of the current logged-in user, enable OTP, and so on."})
public class Password extends ModelBase implements Editable {
    private static final String KEY_IN_SESSION_FLAG = "keyForOtp";

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
            type = FieldType.bool,
            name = {"修改密码", "en:Change Password"},
            info = {"标记需要本用户登录系统的密码。", "en:Mark the password that requires the user to log in to the system."})
    public boolean changePwd = true;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            name = {"原始密码", "en:Original Password"},
            info = {"登录系统的原始密码。", "en:The original password to log in to the system."})
    public String originalPassword;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            name = {"新密码", "en:Password"},
            info = {"用于登录系统的新密码。", "en:The new password used to log in to the system."})
    public String newPassword;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认用于登录系统的新密码。", "en:Confirm the new password used to log in to the system."})
    public String confirmPassword;

    @ModelField(
            required = false, type = FieldType.bool,
            list = true,
            name = {"动态密码", "en:One-time password"},
            info = {"用户开启动态密码，在登录系统时，输入动态密码，可免输入账户密码。",
                    "en:When the user turns on the one-time password, when logging in to the system, enter the one-time password, and the account password is not required."}
    )
    public Boolean enableOtp = true;

    @Override
    public void start() {
        appContext.addI18n("keyForOtp.bind", new String[]{"请先刷新动态密码", "en:Please refresh the one-time password"});
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
        Password password = new Password();
        Map<String, String> loginUserPro = getDataStore().getDataById(request.getUser());
        password.enableOtp = Boolean.parseBoolean(loginUserPro.get("enableOtp"));
        response.addModelData(password);
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    public void show(Request request, Response response) throws Exception {
        edit(request, response);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新密码。", "en:Update the password."})
    public void update(Request request, Response response) throws Exception {
        String loginUser = request.getUser();
        Map<String, String> loginUserPro = getDataStore().getDataById(loginUser);

        if (Boolean.parseBoolean(request.getParameter("changePwd"))) {
            String error = checkPwdParam(request, loginUserPro);
            if (error != null) {
                response.setSuccess(false);
                response.setMsg(error);
                return;
            }

            String[] passwords = splitPwd(loginUserPro.get("password"));
            String digestAlg = passwords[0];
            int saltLength = Integer.parseInt(passwords[1]);
            int iterations = Integer.parseInt(passwords[2]);
            MessageDigest digest = appContext.getService(CryptoService.class).getMessageDigest();
            loginUserPro.put(User.pwdKey, digest.digest(request.getParameter("newPassword"), digestAlg, saltLength, iterations));
            loginUserPro.put("changePwd", "false");
            User.insertPasswordModifiedTime(loginUserPro);
            Console console = MasterApp.getService(Config.class).getConsole();
            String historyPasswords = User.cutOldPasswords(
                    loginUserPro.remove("historyPasswords"),
                    console.getSecurity().getPasswordLimitRepeats(), loginUserPro.get(User.pwdKey));
            loginUserPro.put("historyPasswords", historyPasswords);
        }

        String enableOtpFlag = request.getParameter("enableOtp");
        if (enableOtpFlag != null) {
            boolean parsedBoolean = Boolean.parseBoolean(enableOtpFlag);
            loginUserPro.put("enableOtp", String.valueOf(parsedBoolean));

            String keyForOtp = loginUserPro.get("keyForOtp");
            if (parsedBoolean && Utils.isBlank(keyForOtp)) {
                response.setSuccess(false);
                response.setMsg(appContext.getI18n(request.getLang(), "keyForOtp.bind"));
                return;
            }
        }

        getDataStore().updateDataById(loginUser, loginUserPro);
    }

    private String checkPwdParam(Request request, Map<String, String> loginUserPro) {
        String loginUser = request.getUser();

        String newValue = request.getParameter("originalPassword");
        if (newValue == null) { // fix #ITAIT-2849
            return appContext.getI18n(request.getLang(), "validator.require");
        }
        String pwd = loginUserPro.get(User.pwdKey);
        MessageDigest digest = appContext.getService(CryptoService.class).getMessageDigest();
        if (!digest.matches(newValue, pwd)) {
            return appContext.getI18n(request.getLang(), "password.original.failed");
        }

        newValue = request.getParameter("newPassword");
        if (newValue == null) { // fix #ITAIT-2849
            return appContext.getI18n(request.getLang(), "validator.require");
        }
        String msg = User.checkPwd(appContext, request.getLang(), newValue, loginUser);
        if (msg != null) {
            return msg;
        }
        boolean matches = digest.matches(newValue, pwd);
        if (matches) {
            return appContext.getI18n(request.getLang(), "password.change.not");
        }

        newValue = request.getParameter("confirmPassword");
        if (newValue == null) { // fix #ITAIT-2849
            return appContext.getI18n(request.getLang(), "validator.require");
        }
        if (!newValue.equals(request.getParameter("newPassword"))) {
            return appContext.getI18n(request.getLang(), "password.confirm.failed");
        }

        String historyPasswords = loginUserPro.get("historyPasswords");
        if (null != historyPasswords && !historyPasswords.isEmpty()) {
            for (String oldPass : historyPasswords.split(",")) {
                if (!oldPass.isEmpty() && digest.matches(newValue, oldPass)) {
                    int limitRepeats = MasterApp.getService(Config.class).getConsole().getSecurity().getPasswordLimitRepeats();
                    return String.format(appContext.getI18n(request.getLang(), "password.doNotUseOldPasswords"), limitRepeats);
                }
            }
        }

        return null;
    }

    @ModelAction(icon = "shield", name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"获取当前用户的动态密码，以二维码形式提供给用户。", "en:Obtain the current user OTP and provide it to the user in the form of a QR code."})
    public void refreshKey(Request request, Response response) throws Exception {
        String keyForOtp = Totp.randomSecureKey();
        request.setParameterInSession(KEY_IN_SESSION_FLAG, keyForOtp);

        String loginUser = request.getUser();
        String qrCode = Totp.buildTotpLink(loginUser, keyForOtp);
        String format = "PNG";
        response.setContentType("image/" + format);

        QrGenerator qrGenerator = appContext.getService(QrGenerator.class);
        byte[] bytes = qrGenerator.generateQrImage(qrCode, format, 9, 4, 0xE0F0FF, 0x404040);
        // 二维码返回到浏览器
        String body = appContext.getService(CryptoService.class).getHexCoder().bytesToHex(bytes);
        response.addData(new HashMap<String, String>() {{
            put("qrImage", body);
        }});
    }

    @ModelAction(
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"验证并刷新动态密码。", "en:Verify and refresh the OTP."})
    public void confirmKey(Request request, Response response) throws Exception {
        boolean result = false;
        String reqCode = request.getParameter("otp");
        if (null != reqCode && !reqCode.isEmpty()) {
            String keyForOtp = request.getParameterInSession(KEY_IN_SESSION_FLAG);
            result = Totp.verify(keyForOtp, reqCode);
            if (result) {
                String loginUser = request.getUser();
                Map<String, String> attributes = getDataStore().getDataById(loginUser);
                attributes.put("keyForOtp", keyForOtp);
                getDataStore().updateDataById(loginUser, attributes);
            }
        }
        response.setSuccess(result);
    }

}
