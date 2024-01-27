package qingzhou.app;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        appContext.setDataStore(new MemoryDataStore());
    }
}
