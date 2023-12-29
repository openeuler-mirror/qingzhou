package qingzhou.console.impl;

import qingzhou.api.AppContext;
import qingzhou.api.Constants;
import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.ModelManager;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.AppInfoManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;
import java.nio.file.Paths;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static File getAppDir() {
        return new File(fc.getDomain(), "apps");
    }

    public static ConsoleContext getMasterAppConsoleContext() {
        try {
            return getAppContext(Constants.MASTER_APP_NAME).getConsoleContext();
        } catch (Exception e) {
            return null;
        }
    }

    public static ConsoleContext getAppConsoleContext(String appName) {
        return getAppContext(appName).getConsoleContext();
    }

    public static ModelManager getAppModelManager(String appName) {
        return getAppConsoleContext(appName).getModelManager();
    }

    public static AppContext getAppContext(String appName) {
        return getAppInfoManager().getAppInfo(appName).getAppContext();
    }

    public static void invokeAction(String appName, Request request, Response response) throws Exception {
        getAppInfoManager().getAppInfo(appName).invokeAction(request, response);
    }

    public static AppInfoManager getAppInfoManager() {
        return fc.getAppInfoManager();
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

    public static File getDomain() {
        return fc.getDomain();
    }

    public static File getHome() {
        return fc.getHome();
    }

    public static File getLogs() {
        return new File(getDomain(), "logs");
    }

    public static File getServerXml() {
        return Paths.get(getDomain().getAbsolutePath(), "conf", "qingzhou.xml").toFile();
    }

    public static Logger getLogger() {
        return fc.getService(LoggerService.class).getLogger();
    }

    private ConsoleWarHelper() {
    }
}
