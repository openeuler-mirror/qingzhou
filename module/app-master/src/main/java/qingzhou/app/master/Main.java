package qingzhou.app.master;

import org.osgi.framework.BundleContext;
import qingzhou.api.*;
import qingzhou.app.App;
import qingzhou.app.QingZhouSystemApp;
import qingzhou.framework.Framework;

public class Main extends QingZhouSystemApp {
    private static Framework F;
    private static BundleContext BC;

    @Override
    public void start(AppContext appContext) {
        F = this.framework;
        BC = this.bundleContext;

        appContext.getConsoleContext().addI18N("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});

        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 2);
        consoleContext.setMenuInfo("Guide", new String[]{"用户指引", "en:Guide"}, "hand-up", 3);

        appContext.setDefaultDataStore(new ConsoleDataStore());

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static <T> T getService(Class<T> type) {
        return BC.getService(BC.getServiceReference(type));
    }

    public static Framework getFramework() {
        return F;
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public String doFilter(Request request, Response response, AppContext appContext) {
            if (App.SYS_MODEL_NODE.equals(request.getModelName())
                    && App.SYS_NODE_LOCAL.equals(request.getId())) {
                if (EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())
                        || DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    return appContext.getConsoleContext().getI18N(request.getI18nLang(), "validator.master.system");
                }
            }

            return null;
        }
    }
}
