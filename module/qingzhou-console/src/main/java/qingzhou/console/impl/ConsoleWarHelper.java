package qingzhou.console.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static File getAppDir() {
        return new File(fc.getDomain(), "apps");
    }

    public static AppContext getAppContext(String appName) {
        return getAppInfoManager().getAppInfo(appName).getAppContext();
    }

    public static AppManager getAppInfoManager() {
        return fc.getAppManager();
    }

    public static MessageDigest getMessageDigest() {
        return fc.getService(CryptoService.class).getMessageDigest();
    }

    public static PasswordCipher getPasswordCipher(String key) {
        return fc.getService(CryptoService.class).getPasswordCipher(key);
    }

    public static CryptoService getCryptoService() {
        return fc.getService(CryptoService.class);
    }

    public static File getUploadDir() {
        return fc.getCache();
    }

    public static File getCache() {
        return fc.getCache();
    }

    public static File getLibDir() {
        return fc.getLib();
    }

    public static File getHome() {
        return fc.getHome();
    }

    public static Logger getLogger() {
        return fc.getService(LoggerService.class).getLogger();
    }

    private ConsoleWarHelper() {
    }
}
