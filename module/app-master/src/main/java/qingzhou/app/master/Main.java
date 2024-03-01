package qingzhou.app.master;

import qingzhou.api.*;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.App;
import qingzhou.framework.app.QingZhouSystemApp;

public class Main extends QingZhouSystemApp {
    private static FrameworkContext FC;

    @Override
    public void start(AppContext appContext) {
        FC = this.frameworkContext;

        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});

        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);
        appContext.addMenu("Guide", new String[]{"用户指引", "en:Guide"}, "hand-up", 3);

        appContext.setDefaultDataStore(new ConsoleDataStore());

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static <T> T getService(Class<T> type) {
        return FC.getServiceManager().getService(type);
    }

    public static FrameworkContext getFramework() {
        return FC;
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public String doFilter(Request request, Response response, AppContext appContext) {
            if (App.SYS_MODEL_NODE.equals(request.getModelName())
                    && App.SYS_NODE_LOCAL.equals(request.getId())) {
                if (EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())
                        || DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    return appContext.getMetadata().getI18n(request.getI18nLang(), "validator.master.system");
                }
            }

            return null;
        }
    }
}
