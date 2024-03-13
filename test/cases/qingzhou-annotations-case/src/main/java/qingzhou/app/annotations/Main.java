package qingzhou.app.annotations;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;

@App
public class Main extends QingzhouApp {
    @Override
    public void start(AppContext appContext) {
        System.out.println("quinzhou");
    }
}
