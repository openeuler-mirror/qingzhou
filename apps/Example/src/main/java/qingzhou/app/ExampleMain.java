package qingzhou.app;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.logger.Logger;

@App
public class ExampleMain implements QingzhouApp {
    public static final String MAIN_MENU = "Main_Menu";
    public static AppContext appContext;

    private Logger logger;

    @Override
    public void start(AppContext appContext) {
        ExampleMain.appContext = appContext;

        logger = appContext.getService(Logger.class);
        logger.info("启动样例应用");

        appContext.addMenu(MAIN_MENU, new String[]{"主菜单", "en: Main Menu"}, "cog", 1);

        appContext.addActionFilter(request -> {
            String msg = String.format("有请求进入，模块：%s，操作：%s", request.getModel(), request.getAction());
            logger.info(msg);
            return null; // null 表示无异常
        });
    }

    @Override
    public void stop() {
        logger.info("应用已停止运行");
    }
}
