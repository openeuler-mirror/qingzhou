package qingzhou.app;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;

import java.io.File;

public class Main implements QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        System.out.println("Load app: " + new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName());
    }
}
