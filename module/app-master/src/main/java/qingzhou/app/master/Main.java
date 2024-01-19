package qingzhou.app.master;

import qingzhou.app.master.service.Node;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DeleteModel;
import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.impl.FrameworkContextImpl;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("Security", new String[]{"安全管理", "en:Security"}, "shield", 2);
        consoleContext.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 3);

        appContext.setDataStore(new ConsoleDataStore());

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static FrameworkContext getFC() {
        return FrameworkContextImpl.getFrameworkContext();
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
