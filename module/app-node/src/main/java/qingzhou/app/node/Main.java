package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.QingZhouSystemApp;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;

public class Main extends QingZhouSystemApp { // todo： 节点支持“管理”，可查看节点的状态信息
    private static FrameworkContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.frameworkContext;
        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("Monitor", new String[]{"监视管理", "en:Monitor"}, "line-chart", 2);
    }

    public static FrameworkContext getFc() {
        return fc;
    }
}
