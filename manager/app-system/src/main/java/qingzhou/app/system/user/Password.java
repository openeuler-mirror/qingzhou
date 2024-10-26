package qingzhou.app.system.user;

import qingzhou.api.*;
import qingzhou.api.type.Export;
import qingzhou.api.type.Update;
import qingzhou.app.system.Main;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.TotpCipher;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;
import qingzhou.qr.QrGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Model(code = DeployerConstants.MODEL_PASSWORD, icon = "key",
        hidden = true,
        menu = Main.Setting,
        entrance = Update.ACTION_EDIT,
        name = {"密码", "en:Password"},
        info = {"用于修改当前登录用户的密码、动态密码等。",
                "en:It is used to change the password of the current logged-in user, enable OTP, and so on."})
public class Password extends ModelBase implements Update, Export {
    private final String KEY_IN_SESSION_FLAG = "keyForOtp";

    @ModelField(
            input_type = InputType.bool,
            name = {"修改密码", "en:Change Password"},
            info = {"标记需要本用户登录系统的密码。", "en:Mark the password that requires the user to log in to the system."})
    public Boolean changePwd;

    @ModelField(
            display = "changePwd=true",
            input_type = InputType.password,
            required = true,
            name = {"原始密码", "en:Original Password"},
            info = {"登录系统的原始密码。", "en:The original password to log in to the system."})
    public String originalPassword;

    @ModelField(
            display = "changePwd=true",
            input_type = InputType.password,
            required = true,
            name = {"新密码", "en:New Password"},
            info = {"用于登录系统的新密码。", "en:The new password used to log in to the system."})
    public String newPassword;

    @ModelField(
            display = "changePwd=true",
            input_type = InputType.password,
            required = true,
            name = {"确认密码", "en:Confirm Password"},
            info = {"确认用于登录系统的新密码。", "en:Confirm the new password used to log in to the system."})
    public String confirmPassword;

    @ModelField(
            input_type = InputType.bool,
            name = {"动态密码", "en:One-time password"},
            info = {"用户开启动态密码，在登录系统时，输入动态密码，可免输入账户密码。",
                    "en:When the user turns on the one-time password, when logging in to the system, enter the one-time password, and the account password is not required."})
    public Boolean enableOtp = true;

    @Override
    public void start() {
        getAppContext().addI18n("keyForOtp.bind", new String[]{"请先刷新动态密码",
                "en:Please refresh the one-time password"});
        getAppContext().addI18n("password.confirm.failed", new String[]{"确认密码与新密码不一致",
                "en:Confirm that the password does not match the new password"});
        getAppContext().addI18n("password.original.failed", new String[]{"原始密码错误",
                "en:The original password is wrong"});
        getAppContext().addI18n("password.change.not", new String[]{"新密码与原始密码是一样的，没有发生改变",
                "en:The new password is the same as the original password and has not changed"});
        getAppContext().addI18n("password.doNotUseOldPasswords", new String[]{"出于安全考虑，请勿设置最近使用过的密码",
                "en:For security reasons, do not set a recently used password"});
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
            code = DeployerConstants.ACTION_CONFIRMKEY,
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"验证并刷新动态密码。", "en:Verify and refresh the OTP."})
    public void confirmKey(Request request) throws Exception {
        boolean result = false;
        String reqCode = request.getParameter("otp");
        if (Utils.notBlank(reqCode)) {
            String keyForOtp = request.getParameterInSession(KEY_IN_SESSION_FLAG);
            TotpCipher totpCipher = getAppContext().getService(CryptoService.class).getTotpCipher();
            result = totpCipher.verifyCode(keyForOtp, reqCode);
            if (result) {
                Map<String, String> data = new HashMap<>();
                data.put(User.idKey, request.getUser());
                data.put("keyForOtp", keyForOtp);
                User.updateDataForUser(data);
                request.removeParameterInSession(KEY_IN_SESSION_FLAG);
            }
        }
        request.getResponse().setSuccess(result);
    }

    @ModelAction(
            code = Export.ACTION_EXPORT,
            icon = "shield",
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"获取当前用户的动态密码，以二维码形式提供给用户。", "en:Obtain the current user OTP and provide it to the user in the form of a QR code."})
    public void refreshKey(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    @Override
    public StreamSupplier exportData(String id) {
        Request request = getAppContext().getCurrentRequest();
        return new StreamSupplier() {
            private final String format = "png";

            @Override
            public byte[] read(long offset) throws IOException {
                TotpCipher totpCipher = Main.getService(CryptoService.class).getTotpCipher();
                String keyForOtp = totpCipher.generateKey();
                request.setParameterInSession(KEY_IN_SESSION_FLAG, keyForOtp);

                request.getResponse().setContentType("image/" + format);

                String loginUser = request.getUser();
                String qrCode = "otpauth://totp/" + loginUser + "?secret=" + keyForOtp;
                QrGenerator qrGenerator = Main.getService(QrGenerator.class);
                return qrGenerator.generateQrImage(qrCode, format, 9, 4, 0xE0F0FF, 0x404040);
            }

            @Override
            public long offset() {
                return -1L;
            }

            @Override
            public String getDownloadName() {
                return "keyForOtp." + format;
            }
        };
    }

    @Override
    public Map<String, String> editData(String id) {
        Request request = getAppContext().getCurrentRequest();
        Map<String, String> loginUserPro = User.showDataForUserInternal(request.getUser());
        boolean enableOtp = Boolean.parseBoolean(Objects.requireNonNull(loginUserPro).get("enableOtp"));
        return new HashMap<String, String>() {{
            put("changePwd", "true");
            put("enableOtp", String.valueOf(enableOtp));
        }};
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Request request = getAppContext().getCurrentRequest();
        String loginUser = request.getUser();
        Map<String, String> baseData = Objects.requireNonNull(User.showDataForUserInternal(loginUser));

        if (Boolean.parseBoolean(request.getParameter("changePwd"))) {
            String error = checkError(request, baseData);
            if (error != null) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(getAppContext().getI18n(error));
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
                request.getResponse().setMsg(getAppContext().getI18n("keyForOtp.bind"));
                return;
            }
        }

        User.updateDataForUser(baseData);
    }

    @Override
    public String[] formActions() {
        return new String[]{Export.ACTION_EXPORT};
    }
}
