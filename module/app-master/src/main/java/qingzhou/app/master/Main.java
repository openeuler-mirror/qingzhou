package qingzhou.app.master;

import qingzhou.app.master.service.Node;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.*;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("User", new String[]{"用户管理", "en:User"}, "user", 2);

        File serverXml = FileUtil.newFile(appContext.getDomain(), "conf", "server.xml");
        appContext.setDataStore(new ConsoleDataStore(serverXml));

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public boolean doFilter(Request request, Response response) {
            if (Node.MODEL_NAME.equals(request.getModelName())
                    && FrameworkContext.LOCAL_NODE_NAME.equals(request.getId())) {
                return !EditModel.ACTION_NAME_UPDATE.equals(request.getActionName())
                        && !DeleteModel.ACTION_NAME_DELETE.equals(request.getActionName());
            }

            return true;
        }
    }
}
