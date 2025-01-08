package qingzhou.app.master;

import java.io.File;
import java.util.Map;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.console.Console;
import qingzhou.config.console.impl.Config;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.QingzhouSystemApp;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.engine.ModuleContext;
import qingzhou.json.Json;

@App
public class Main extends QingzhouSystemApp {
    public static final String QZ_VER_NAME = "version";

    public static final String Business = "Business";
    public static final String Setting = "Setting";
    public static final String Service = "Service";
    private static Main main;
    private static Config fileConfig;

    public static Config getConfig() {
        return fileConfig;
    }

    public static Console getConsole() {
        return fileConfig.getConsole();
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) main.moduleContext;

        return main.moduleContext.getService(type);
    }

    public static File getLibBase() {
        return Main.getService(ModuleContext.class).getLibDir().getParentFile();
    }

    public static void invokeAgentOnInstances(Request request, String action, String[] instances) {
        RequestImpl requestImpl = (RequestImpl) request;
        String originModel = request.getModel();
        String originAction = request.getAction();
        try {
            requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
            requestImpl.setActionName(action);
            Map<String, Response> responseList = Main.getService(ActionInvoker.class)
                    .invokeAll(request, instances);
            requestImpl.setInvokeOnInstances(responseList);
        } finally {
            requestImpl.setModelName(originModel);
            requestImpl.setActionName(originAction);
        }
    }

    @Override
    public void start(AppContext appContext) {
        main = this;
        fileConfig = new Config(getService(Json.class),
                new File(new File(main.moduleContext.getInstanceDir(), "conf"), "qingzhou.json"));

        appContext.addMenu(Main.Business, new String[]{"业务管理", "en:" + Main.Business}).icon("th-large").order("1");
        appContext.addMenu(Main.Service, new String[]{"开放服务", "en:" + Main.Service}).icon("folder-open").order("2");
        appContext.addMenu(Main.Setting, new String[]{"系统设置", "en:" + Main.Setting}).icon("cog").order("3");
    }
}
