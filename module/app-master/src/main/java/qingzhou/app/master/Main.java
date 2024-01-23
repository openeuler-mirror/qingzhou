package qingzhou.app.master;

import qingzhou.app.master.service.Node;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.QingZhouSystemApp;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DeleteModel;
import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class Main extends QingZhouSystemApp {
    private static FrameworkContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.frameworkContext;
        appContext.getConsoleContext().addI18N("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});

        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("User", new String[]{"用户管理", "en:User"}, "user", 2);

        File serverXml = FileUtil.newFile(appContext.getDomain(), "conf", "server.xml");
        appContext.setDataStore(new ConsoleDataStore(serverXml));

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static FrameworkContext getFc() {
        return fc;
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public boolean doFilter(Request request, Response response, AppContext appContext) {
            if (Node.MODEL_NAME.equals(request.getModelName())
                    && FrameworkContext.LOCAL_NODE_NAME.equals(request.getId())) {
                if (EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())
                        || DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    response.setMsg(appContext.getConsoleContext().getI18N(request.getI18nLang(), "validator.master.system"));
                    return false;
                }
            }

            return true;
        }
    }
}
