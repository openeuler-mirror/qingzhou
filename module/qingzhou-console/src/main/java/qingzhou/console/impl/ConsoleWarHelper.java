package qingzhou.console.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.File;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static AppManager getAppManager() {
        return fc.getAppManager();
    }

    public static Serializer getSerializer() {
        return fc.getService(SerializerService.class).getSerializer();
    }

    public static CryptoService getCryptoService() {
        return fc.getService(CryptoService.class);
    }

    public static File getCache() {
        return fc.getCache();
    }

    public static File getLib() {
        return fc.getLib();
    }

    public static File getDomain() {
        return fc.getDomain();
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
