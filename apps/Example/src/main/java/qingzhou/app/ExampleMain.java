package qingzhou.app;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.app.model.Department;
import qingzhou.logger.LogService;
import qingzhou.logger.Logger;

import java.util.Properties;

@App
public class ExampleMain implements QingzhouApp {
    public static final String MENU_1 = "MENU_1";
    public static final String MENU_11 = "MENU_11";

    public static Logger logger;

    public ExampleMain() {
    }
    public ExampleMain(Properties properties) {
    }

    @Override
    public void start(AppContext appContext) {
        LogService service = appContext.getService(LogService.class);
        logger = service.getLogger(ExampleMain.class);
        logger.info("启动样例应用");

        appContext.addMenu(MENU_1, new String[]{"一级菜单", "en:1"}).icon("folder-open").order(1);
        appContext.addMenu(MENU_11, new String[]{"二级菜单", "en:11"}).icon("leaf").order(1).parent(MENU_1).model(Department.code).action(Department.ACTION_MENUHEALTHCHECK);

        appContext.addActionFilter(request -> {
            String msg = String.format("有请求进入，模块：%s，操作：%s", request.getModel(), request.getAction());
            logger.debug(msg);
            return null; // null 表示无异常
        });
    }

    @Override
    public void stop() {
        System.out.println("停止样例应用");
    }
}
