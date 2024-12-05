package qingzhou.app.system;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.app.system.oauth2.OAuth2Adapter;
import qingzhou.config.Config;
import qingzhou.config.OAuth2;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.QingzhouSystemApp;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.engine.ModuleContext;

import java.io.File;
import java.util.Map;

@App
public class Main extends QingzhouSystemApp {
    public static final String QZ_VER_NAME = "version";

    public static final String Business = "Business";
    public static final String Setting = "Setting";
    public static final String Service = "Service";
    private static Main main;

    @Override
    public void start(AppContext appContext) {
        main = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});

        appContext.addMenu(Main.Business, new String[]{"业务管理", "en:" + Main.Business}).icon("th-large").order("1");
        appContext.addMenu(Main.Setting, new String[]{"系统设置", "en:" + Main.Setting}).icon("cog").order("2");
        appContext.addMenu(Main.Service, new String[]{"开放服务", "en:" + Main.Service}).icon("cubes").order("3");

        Config config = Main.getService(Config.class);
        OAuth2 oAuth2 = config.getCore().getConsole().getOAuth2();
        if (oAuth2 != null && oAuth2.isEnabled()) {
            appContext.setAuthAdapter(new OAuth2Adapter(oAuth2));
        }
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
                    .invokeOnInstances(request, instances);
            requestImpl.setInvokeOnInstances(responseList);
        } finally {
            requestImpl.setModelName(originModel);
            requestImpl.setActionName(originAction);
        }
    }
}
