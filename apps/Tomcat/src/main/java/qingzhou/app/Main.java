package qingzhou.app;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;

import java.io.File;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        appContext.getConsoleContext().setMenuInfo("tomcat-service", new String[]{"Tomcat 服务管理。", "en:Tomcat service management."}, "layout", 1);
        appContext.setDataStore(new ServiceDataStore());
        System.out.println("Load app: " + new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName());
    }
}
