package qingzhou.app.annotations;

import qingzhou.api.AppContext;
import qingzhou.api.QingZhou;
import qingzhou.api.QingZhouApp;
@QingZhou
public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        System.out.println("quinzhou");
    }
}
