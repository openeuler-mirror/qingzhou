package qingzhou.app;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.logger.Logger;

@App
public class ExampleMain implements QingzhouApp {
    public static final String SYSTEM_MANAGEMENT = "SYSTEM_MANAGEMENT";
    public static AppContext appContext;

    private Logger logger;

    @Override
    public void start(AppContext appContext) {
        ExampleMain.appContext = appContext;

        logger = appContext.getService(Logger.class);
        logger.info("启动样例应用");

        appContext.addMenu(SYSTEM_MANAGEMENT, new String[]{"系统管理", "en: System Management"}, "cog", 1);

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
