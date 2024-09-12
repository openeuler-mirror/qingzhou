package qingzhou.app.system.setting;

import qingzhou.api.*;
import qingzhou.api.type.Updatable;
import qingzhou.app.system.Main;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.TotpCipher;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;
import qingzhou.qr.QrGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Model(code = DeployerConstants.MODEL_PASSWORD, icon = "key",
        menu = Main.SETTING_MENU, order = 2,
        entrance = Updatable.ACTION_EDIT,
        name = {"密码", "en:Password"},
        info = {"用于修改当前登录用户的密码、动态密码等。",
                "en:It is used to change the password of the current logged-in user, enable OTP, and so on."})
public class Password extends ModelBase {
    private final String KEY_IN_SESSION_FLAG = "keyForOtp";

    @ModelField(
            type = FieldType.bool,
            name = {"修改密码", "en:Change Password"},
            info = {"标记需要本用户登录系统的密码。", "en:Mark the password that requires the user to log in to the system."})
    public Boolean changePwd = true;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            required = true,
            name = {"原始密码", "en:Original Password"},
            info = {"登录系统的原始密码。", "en:The original password to log in to the system."})
    public String originalPassword;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            required = true,
            name = {"新密码", "en:New Password"},
            info = {"用于登录系统的新密码。", "en:The new password used to log in to the system."})
    public String newPassword;

    @ModelField(
            show = "changePwd=true",
            type = FieldType.password,
            required = true,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认用于登录系统的新密码。", "en:Confirm the new password used to log in to the system."})
    public String confirmPassword;

    @ModelField(
            type = FieldType.bool,
            list = true,
            name = {"动态密码", "en:One-time password"},
            info = {"用户开启动态密码，在登录系统时，输入动态密码，可免输入账户密码。",
                    "en:When the user turns on the one-time password, when logging in to the system, enter the one-time password, and the account password is not required."})
    public Boolean enableOtp = true;

    @Override
    public void start() {
        appContext.addI18n("keyForOtp.bind", new String[]{"请先刷新动态密码",
                "en:Please refresh the one-time password"});
        appContext.addI18n("password.confirm.failed", new String[]{"确认密码与新密码不一致",
                "en:Confirm that the password does not match the new password"});
        appContext.addI18n("password.original.failed", new String[]{"原始密码错误",
                "en:The original password is wrong"});
        appContext.addI18n("password.change.not", new String[]{"新密码与原始密码是一样的，没有发生改变",
                "en:The new password is the same as the original password and has not changed"});
        appContext.addI18n("password.doNotUseOldPasswords", new String[]{"出于安全考虑，请勿设置最近使用过的密码",
                "en:For security reasons, do not set a recently used password"});
    }

    @ModelAction(
            code = Updatable.ACTION_EDIT,
            name = {"修改", "en:Edit"},
            info = {"修改当前登录账户的密码。",
                    "en:Change the password of the current login account."})
    public void edit(Request request) throws Exception {
        Password password = new Password();
        Map<String, String> loginUserPro = User.showDataForUser(request.getUser());
        password.enableOtp = Boolean.parseBoolean(Objects.requireNonNull(loginUserPro).get("enableOtp"));
        request.getResponse().addModelData(password);
    }

    @ModelAction(
            code = Updatable.ACTION_UPDATE,
            name = {"更新", "en:Update"},
            info = {"更新密码。", "en:Update the password."})
    public void update(Request request) throws Exception {
        String loginUser = request.getUser();
        Map<String, String> baseData = Objects.requireNonNull(User.showDataForUser(loginUser));

        if (Boolean.parseBoolean(request.getParameter("changePwd"))) {
            String error = checkError(request, baseData);
            if (error != null) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(appContext.getI18n(error));
                return;
            }

            String[] passwords = User.splitPwd(baseData.get("password"));
            String digestAlg = passwords[0];
            int saltLength = Integer.parseInt(passwords[1]);
            int iterations = Integer.parseInt(passwords[2]);
            MessageDigest digest = Main.getService(CryptoService.class).getMessageDigest();
            baseData.put("password", digest.digest(request.getParameter("newPassword"), digestAlg, saltLength, iterations));
            baseData.put("changePwd", "false");
            User.insertPasswordModifiedTime(baseData);
            Console console = Main.getService(Config.class).getConsole();
            String historyPasswords = User.cutOldPasswords(
                    baseData.remove("historyPasswords"),
                    console.getSecurity().getPasswordLimitRepeats(), baseData.get("password"));
            baseData.put("historyPasswords", historyPasswords);
        }

        String enableOtpFlag = request.getParameter("enableOtp");
        if (enableOtpFlag != null) {
            boolean parsedBoolean = Boolean.parseBoolean(enableOtpFlag);
            baseData.put("enableOtp", String.valueOf(parsedBoolean));

            String keyForOtp = baseData.get("keyForOtp");
            if (parsedBoolean && Utils.isBlank(keyForOtp)) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(appContext.getI18n("keyForOtp.bind"));
                return;
            }
        }

        User.updateDataForUser(baseData);
    }

    private String checkError(Request request, Map<String, String> baseData) {
        String oldPwd = baseData.get("password");
        MessageDigest digest = Main.getService(CryptoService.class).getMessageDigest();
        if (!digest.matches(request.getParameter("originalPassword"),
                oldPwd)) return "password.original.failed";

        String newPassword = request.getParameter("newPassword");
        String msg = User.checkPwd(newPassword, request.getUser());
        if (msg != null) return msg;

        boolean matches = digest.matches(newPassword, oldPwd);
        if (matches) return "password.change.not";

        if (!newPassword.equals(request.getParameter("confirmPassword")))
            return "password.confirm.failed";

        String historyPasswords = baseData.get("historyPasswords");
        if (null != historyPasswords && !historyPasswords.isEmpty()) {
            for (String historyPwd : historyPasswords.split(",")) {
                if (!historyPwd.isEmpty() && digest.matches(newPassword, historyPwd)) {
                    return "password.doNotUseOldPasswords";
                }
            }
        }

        return null;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_REFRESHKEY,
            icon = "shield",
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"获取当前用户的动态密码，以二维码形式提供给用户。", "en:Obtain the current user OTP and provide it to the user in the form of a QR code."})
    public void refreshKey(Request request) throws Exception {
        TotpCipher totpCipher = Main.getService(CryptoService.class).getTotpCipher();
        String keyForOtp = totpCipher.generateKey();
        request.setParameterInSession(KEY_IN_SESSION_FLAG, keyForOtp);

        String format = "PNG";
        request.getResponse().setContentType("image/" + format);

        String loginUser = request.getUser();
        String qrCode = "otpauth://totp/" + loginUser + "?secret=" + keyForOtp;
        QrGenerator qrGenerator = Main.getService(QrGenerator.class);
        byte[] bytes = qrGenerator.generateQrImage(qrCode, format, 9, 4, 0xE0F0FF, 0x404040);
        // 二维码返回到浏览器
        String body = Main.getService(CryptoService.class).getBase64Coder().encode(bytes);
        request.getResponse().addData(new HashMap<String, String>() {{
            put("FOR-FileView", body);
        }});
    }

    @ModelAction(
            code = "confirmKey",
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"验证并刷新动态密码。", "en:Verify and refresh the OTP."})
    public void confirmKey(Request request) throws Exception {
        boolean result = false;
        String reqCode = request.getParameter("otp");
        if (Utils.notBlank(reqCode)) {
            String keyForOtp = request.getParameterInSession(KEY_IN_SESSION_FLAG);
            TotpCipher totpCipher = appContext.getService(CryptoService.class).getTotpCipher();
            result = totpCipher.verifyCode(keyForOtp, reqCode);
            if (result) {
                Map<String, String> data = new HashMap<>();
                data.put(User.idKey, request.getUser());
                data.put("keyForOtp", keyForOtp);
                User.updateDataForUser(data);
            }
        }
        request.getResponse().setSuccess(result);
    }
}
