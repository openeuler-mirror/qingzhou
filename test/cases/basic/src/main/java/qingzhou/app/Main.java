package qingzhou.app;

import qingzhou.api.AppContext;
import qingzhou.api.QingZhouApp;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        appContext.setDefaultDataStore(new MemoryDataStore());
    }
}
