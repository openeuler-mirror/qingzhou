package qingzhou.console.impl;

import qingzhou.api.AppContext;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.FrameworkContext;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static AppContext getAppContext(String appName) {
        return fc.getAppInfoManager().getAppInfo(appName).getAppContext();
    }

    public static MessageDigest getMessageDigest() {
        return fc.getService(CryptoService.class).getMessageDigest();
    }

    public static PasswordCipher getPasswordCipher(byte[] key) {
        return fc.getService(CryptoService.class).getPasswordCipher(key);
    }

    public static PasswordCipher getPasswordCipher(String key) {
        return fc.getService(CryptoService.class).getPasswordCipher(key);
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

    public static File getDomain() {
        return fc.getDomain();
    }

    public static File getLogs() {
        return new File(getDomain(), "logs");
    }

    public static Logger getLogger() {
        return fc.getService(LoggerService.class).getLogger();
    }

    private ConsoleWarHelper() {
    }
}
