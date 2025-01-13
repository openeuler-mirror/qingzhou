package qingzhou.app;

import java.util.Arrays;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.app.model.Department;
import qingzhou.app.oauth.TongAuthAdapter;
import qingzhou.logger.Logger;

@App
public class ExampleMain implements QingzhouApp {
    public static final String MENU_1 = "MENU_1";
    public static final String MENU_11 = "MENU_11";

    public static Logger logger;

    @Override
    public void start(AppContext appContext) {
        logger = appContext.getService(Logger.class);
        logger.info("启动样例应用");

        String[] startArgs = appContext.getStartArgs();
        logger.info("启动命令传入的参数：" + Arrays.toString(startArgs));

        appContext.addMenu(MENU_1, new String[]{"一级菜单", "en:1"}).icon("folder-open").order("1");
        appContext.addMenu(MENU_11, new String[]{"二级菜单", "en:11"}).icon("leaf").order("1").parent(MENU_1).action(Department.code, "menuHealthCheck");

        appContext.addAppActionFilter(request -> {
            String msg = String.format("有请求进入，模块：%s，操作：%s", request.getModel(), request.getAction());
            logger.debug(msg);
            return null; // null 表示无异常
        });

        if (Boolean.parseBoolean(appContext.getAppProperties().getProperty("oauth_enabled"))) {
            appContext.setAuthAdapter(new TongAuthAdapter(appContext));
        }
    }

    @Override
    public void stop(AppContext appContext) {
        logger.info("停止样例应用");
    }
}
