package qingzhou.app.master.system.user;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import qingzhou.api.*;
import qingzhou.api.type.Export;
import qingzhou.api.type.Update;
import qingzhou.app.master.Main;
import qingzhou.config.console.Console;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.TotpCipher;
import qingzhou.engine.util.Utils;
import qingzhou.qr.QrGenerator;

@Model(code = DeployerConstants.MODEL_PASSWORD, icon = "key",
        menu = Main.Setting, hidden = true,
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

    @ModelField(input_type = InputType.bool,
            name = {"动态密码", "en:One-time password"},
            info = {"动态密码，是根据专门算法、每隔60秒生成一个不可预测的随机数字组合，是指只能使用一次的密码（One Time Password，简称OTP），又称“一次性口令”。若开启了动态密码，在登录密码不输入或输入错误时，会尝试校验输入的动态密码，以决定是否允许登录。",
                    "en:A one-time password (OTP) is a password that can only be used once, also known as a \"one-time password\", which is an unpredictable random combination of numbers generated every 60 seconds according to a special algorithm. If a one-time password is enabled, the system will try to verify the one-time password if the login password is not entered or entered incorrectly, and the system will try to verify the entered one-time password to determine whether to allow the login."})
    public Boolean enableOtp = true;

    @Override
    public void start() {
        getAppContext().addI18n("refresh.key.need", new String[]{"请先刷新动态密码",
                "en:Please refresh the one-time password"});
        getAppContext().addI18n("password.confirm.failed", new String[]{"确认密码与新密码不一致",
                "en:Confirm that the password does not match the new password"});
        getAppContext().addI18n("password.original.failed", new String[]{"原始密码错误",
                "en:The original password is wrong"});
        getAppContext().addI18n("password.change.not", new String[]{"新密码与原始密码是一样的，没有发生改变",
                "en:The new password is the same as the original password and has not changed"});
        getAppContext().addI18n("password.doNotUseOldPasswords", new String[]{"出于安全考虑，请勿设置最近使用过的密码",
                "en:For security reasons, do not set a recently used password"});

        getAppContext().addModelActionFilter(this, request -> {
            if (Main.getService(Deployer.class).getAuthAdapter() != null) {
                return "Unsupported actions";
            }
            return null;
        });
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
        String reqCode = request.getParameter(DeployerConstants.LOGIN_OTP);
        if (Utils.notBlank(reqCode)) {
            String keyInSession = request.parametersForSession().get(KEY_IN_SESSION_FLAG);
            TotpCipher totpCipher = getAppContext().getService(CryptoService.class).getTotpCipher();
            result = totpCipher.verifyCode(keyInSession, reqCode);
            if (result) {
                Map<String, String> data = new HashMap<>();
                data.put(User.ID_KEY, request.getUser());
                data.put("keyForOtp", keyInSession);
                User.updateDataForUser(data);
                request.parametersForSession().remove(KEY_IN_SESSION_FLAG);
            }
        }
        request.getResponse().setSuccess(result);
    }

    @ModelAction(
            code = Export.ACTION_EXPORT,
            form_action = true,
            action_type = ActionType.qr,
            icon = "shield",
            name = {"刷新动态密码", "en:Refresh OTP"},
            info = {"获取当前用户的动态密码，以二维码形式提供给用户。", "en:Obtain the current user OTP and provide it to the user in the form of a QR code."})
    public void refreshKey(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    @Override
    public DataSupplier exportData(String id) {
        Request request = getAppContext().getCurrentRequest();
        return new DataSupplier() {
            private final String format = "png";

            @Override
            public byte[] read(long offset) throws IOException {
                TotpCipher totpCipher = Main.getService(CryptoService.class).getTotpCipher();
                String genKey = totpCipher.generateKey();
                request.parametersForSession().put(KEY_IN_SESSION_FLAG, genKey);

                request.getResponse().setContentType("image/" + format);

                String loginUser = request.getUser();
                String qrCode = "otpauth://totp/" + loginUser + "?secret=" + genKey;
                QrGenerator qrGenerator = Main.getService(QrGenerator.class);
                return qrGenerator.generateQrImage(qrCode, format, 9, 4, 0xE0F0FF, 0x404040);
            }

            @Override
            public long offset() {
                return -1L;
            }

            @Override
            public String name() {
                return "key." + format;
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
        RequestImpl request = (RequestImpl) getAppContext().getCurrentRequest();
        ResponseImpl response = (ResponseImpl) request.getResponse();

        String loginUser = request.getUser();
        Map<String, String> baseData = Objects.requireNonNull(User.showDataForUserInternal(loginUser));

        if (Boolean.parseBoolean(request.getParameter("changePwd"))) {
            String error = checkError(request, baseData);
            if (error != null) {
                response.setSuccess(false);
                response.setMsg(getAppContext().getI18n(error));
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
            Console console = Main.getConsole();
            String historyPasswords = User.cutOldPasswords(
                    baseData.remove("historyPasswords"), User.PASSWORD_SP,
                    console.getSecurity().getPasswordLimitRepeats(), baseData.get("password"));
            baseData.put("historyPasswords", historyPasswords);
        }

        String enableOtpFlag = request.getParameter("enableOtp");
        if (enableOtpFlag != null) {
            boolean enabled = Boolean.parseBoolean(enableOtpFlag);
            baseData.put("enableOtp", String.valueOf(enabled));

            String oldKey = baseData.get("keyForOtp");
            if (enabled && Utils.isBlank(oldKey)) {
                response.setSuccess(false);
                response.setMsg(getAppContext().getI18n("refresh.key.need"));
                return;
            }
        }

        User.updateDataForUser(baseData);
        response.logout();
    }
}
