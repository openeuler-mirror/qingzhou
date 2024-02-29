package qingzhou.console.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.AppStub;
import qingzhou.console.AppStubManager;
import qingzhou.framework.app.App;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.serializer.Serializer;

import java.io.File;

public class ConsoleWarHelper {
    public static Config getConfig() {
        return Controller.config;
    }

    public static App getLocalApp(String appName) {
        return Controller.appManager.getApp(appName);
    }

    public static AppStub getAppStub(String appName) {
        return AppStubManager.getInstance().getAppStub(appName);
    }

    public static void invokeLocalApp(String appName, Request request, Response response) throws Exception {
        Controller.appManager.getApp(appName).invoke(request, response);
    }

    public static Serializer getSerializer() {
        return Controller.serializer;
    }

    public static CryptoService getCryptoService() {
        return Controller.cryptoService;
    }

    public static File getCache(String subName) {
        return Controller.framework.getTemp(subName);
    }

    public static Logger getLogger() {
        return Controller.logger;
    }

    private ConsoleWarHelper() {
    }
}
