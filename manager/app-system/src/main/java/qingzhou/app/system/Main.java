package qingzhou.app.system;

import java.io.File;
import java.util.List;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.ModuleContext;

@App
public class Main extends QingzhouSystemApp {
    public static final String QZ_VER_NAME = "version";
    public static final String SERVICE_MENU = "Service";
    public static final String SETTING_MENU = "Setting";
    private static Main main;

    @Override
    public void start(AppContext appContext) {
        main = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});

        appContext.addMenu(Main.SERVICE_MENU, new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu(Main.SETTING_MENU, new String[]{"系统设置", "en:System"}, "cog", 3);
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
            List<Response> responseList = Main.getService(ActionInvoker.class)
                    .invokeOnInstances(request, instances);
            final StringBuilder[] error = {null};
            responseList.forEach(response -> {
                if (!response.isSuccess()) {
                    request.getResponse().setSuccess(false);
                    if (error[0] == null) {
                        error[0] = new StringBuilder();
                    }
                    error[0].append(response.getMsg()).append(" ");
                }
            });

            if (!request.getResponse().isSuccess()) {
                String errorMsg = error[0].toString();
                request.getResponse().setMsg(errorMsg);
            }
        } finally {
            requestImpl.setModelName(originModel);
            requestImpl.setActionName(originAction);
        }
    }
}
