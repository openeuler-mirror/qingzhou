package qingzhou.console.impl;

import qingzhou.console.AppStub;
import qingzhou.console.AppStubManager;
import qingzhou.crypto.CryptoService;
import qingzhou.framework.App;
import qingzhou.framework.ConfigManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.File;

public class ConsoleWarHelper {
    static FrameworkContext fc;

    public static ConfigManager getConfigManager() {
        return fc.getConfigManager();
    }

    public static App getLocalApp(String appName) {
        return fc.getAppManager().getApp(appName);
    }

    public static AppStub getAppStub(String appName) {
        return AppStubManager.getInstance().getAppStub(appName);
    }

    public static void invokeLocalApp(String appName, Request request, Response response) throws Exception {
        fc.getAppManager().getApp(appName).invoke(request, response);
    }

    public static Serializer getSerializer() {
        return fc.getServiceManager().getService(SerializerService.class).getSerializer();
    }

    public static CryptoService getCryptoService() {
        return fc.getServiceManager().getService(CryptoService.class);
    }

    public static File getCache(String subName) {
        return fc.getConfigManager().getTemp(subName);
    }

    public static Logger getLogger() {
        return fc.getServiceManager().getService(Logger.class);
    }

    private ConsoleWarHelper() {
    }
}
