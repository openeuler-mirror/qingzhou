package qingzhou.app.master;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.util.ServerUtil;

public class Main implements QingZhouApp {
    @Override
    public void start(AppContext appContext) throws Exception {
        ConsoleContext master = ServerUtil.getMasterConsoleContext();
        master.setMenuInfo("Favorites", new String[]{"我的收藏", "en:Favorites"}, "star", 0);
        master.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        master.setMenuInfo("Product", new String[]{"产品管理", "en:Product"}, "book", 2);
        master.setMenuInfo("Security", new String[]{"安全管理", "en:Security"}, "shield", 3);
        master.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 4);
        master.setMenuInfo("Support", new String[]{"扩展支持", "en:Support"}, "rocket", 5);

        master.addI18N("user.not.permission", new String[]{"当前登录用户没有执行此操作的权限。", "en:The currently login user does not have permission to do this"});
    }
}
