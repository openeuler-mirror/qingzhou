package qingzhou.console.login.method;

import qingzhou.config.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.LoginMethod;
import qingzhou.console.login.Parameter;
import qingzhou.crypto.CryptoService;

public class OtpLogin implements LoginMethod {

    @Override
    public User authorize(Parameter parameter) throws Throwable {
        String user = parameter.get(LoginManager.LOGIN_USER);
        String otp = parameter.get(LoginManager.LOGIN_OTP);
        try {
            otp = SystemController.decryptWithConsolePrivateKey(otp, true);
        } catch (Exception ignored) {
        }

        User registeredUser = SystemController.getConsole().getUser(user);
        boolean authorized = registeredUser != null
                && registeredUser.isActive()
                && registeredUser.isEnableOtp()
                && SystemController.getService(CryptoService.class).getTotpCipher().verifyCode(registeredUser.getKeyForOtp(), otp);
        return authorized ? registeredUser : null;
    }
}
