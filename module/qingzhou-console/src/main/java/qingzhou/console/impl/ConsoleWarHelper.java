package qingzhou.console.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppStub;
import qingzhou.framework.api.Logger;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.File;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static void registerApp(String appToken, AppStub appStub) {
        fc.getAppStubManager().registerAppStub(appToken, appStub);
    }

    public static AppStub getAppStub(String appName) {
        return fc.getAppStubManager().getAppStub(appName);
    }

    public static AppManager getAppManager() {
        return fc.getAppManager();
    }

    public static Serializer getSerializer() {
        return fc.getServiceManager().getService(SerializerService.class).getSerializer();
    }

    public static CryptoService getCryptoService() {
        return fc.getServiceManager().getService(CryptoService.class);
    }

    public static File getCache(String subName) {
        return fc.getFileManager().getTemp(subName);
    }

    public static File getLib() {
        return fc.getFileManager().getLib();
    }

    public static File getDomain() {
        return fc.getFileManager().getDomain();
    }

    public static File getHome() {
        return fc.getFileManager().getHome();
    }

    public static Logger getLogger() {
        return fc.getLogger();
    }

    private ConsoleWarHelper() {
    }
}
