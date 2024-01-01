package qingzhou.app.master;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.QingZhouApp;

public class Main implements QingZhouApp {
    @Override
    public void start(AppContext appContext) throws Exception {
        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Favorites", new String[]{"我的收藏", "en:Favorites"}, "star", 0);
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("Product", new String[]{"产品管理", "en:Product"}, "book", 2);
        consoleContext.setMenuInfo("Security", new String[]{"安全管理", "en:Security"}, "shield", 3);
        consoleContext.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 4);
        consoleContext.setMenuInfo("Support", new String[]{"扩展支持", "en:Support"}, "rocket", 5);
    }
}
