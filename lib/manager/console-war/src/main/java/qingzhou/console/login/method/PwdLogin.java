package qingzhou.console.login.method;

import qingzhou.config.console.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.LoginMethod;
import qingzhou.console.login.Parameter;
import qingzhou.crypto.CryptoService;

public class PwdLogin implements LoginMethod {

    @Override
    public User authorize(Parameter parameter) {
        String user = parameter.get(LoginManager.LOGIN_USER);
        String pwd = parameter.get(LoginManager.LOGIN_PASSWORD);
        try {
            pwd = SystemController.decryptWithConsolePrivateKey(pwd, true);
        } catch (Exception ignored) {
        }

        User registeredUser = SystemController.getConsole().getUser(user);
        boolean authorized = registeredUser != null
                && registeredUser.isActive()
                && SystemController.getService(CryptoService.class).getMessageDigest().matches(pwd, registeredUser.getPassword());
        return authorized ? registeredUser : null;
    }
}
