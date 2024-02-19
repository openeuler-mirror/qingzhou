package qingzhou.app.master;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.app.QingZhouSystemApp;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DeleteModel;
import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

public class Main extends QingZhouSystemApp {
    private static FrameworkContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.frameworkContext;
        appContext.getConsoleContext().addI18N("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});

        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 2);
        consoleContext.setMenuInfo("Guide", new String[]{"用户指引", "en:Guide"}, "hand-up", 3);

        appContext.setDefaultDataStore(new ConsoleDataStore());

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static FrameworkContext getFc() {
        return fc;
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public String doFilter(Request request, Response response, AppContext appContext) {
            if (FrameworkContext.SYS_MODEL_NODE.equals(request.getModelName())
                    && FrameworkContext.SYS_NODE_LOCAL.equals(request.getId())) {
                if (EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())
                        || DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    return appContext.getConsoleContext().getI18N(request.getI18nLang(), "validator.master.system");
                }
            }

            return null;
        }
    }
}
