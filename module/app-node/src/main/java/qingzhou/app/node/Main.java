package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.impl.FrameworkContextImpl;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
    }

    public static FrameworkContext getFC() {
        return FrameworkContextImpl.getFrameworkContext();
    }
}
