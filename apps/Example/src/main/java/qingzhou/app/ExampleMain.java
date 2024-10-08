package qingzhou.app;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.logger.Logger;

@App
public class ExampleMain implements QingzhouApp {
    public static final String MENU_1 = "MENU_1";
    public static final String MENU_11 = "MENU_11";
    public static final String MENU_111 = "MENU_111";
    public static final String MENU_2 = "MENU_2";
    public static AppContext appContext;

    public static Logger logger;

    @Override
    public void start(AppContext appContext) {
        ExampleMain.appContext = appContext;

        logger = appContext.getService(Logger.class);
        logger.info("启动样例应用");

        appContext.addMenu(MENU_1, new String[]{"一级菜单1", "en:1"}, "folder-open", 1);
        appContext.addMenu(MENU_2, new String[]{"一级菜单2", "en:2"}, "folder-open", 1);
        appContext.addMenu(MENU_11, new String[]{"二级菜单1", "en:11"}, "leaf", 1, MENU_1);
        appContext.addMenu(MENU_111, new String[]{"三级菜单1", "en:111"}, "leaf", 1, MENU_11, "a");

        appContext.addActionFilter(request -> {
            String msg = String.format("有请求进入，模块：%s，操作：%s", request.getModel(), request.getAction());
            logger.debug(msg);
            return null; // null 表示无异常
        });
    }

    @Override
    public void stop() {
        logger.info("停止样例应用");
    }
}
