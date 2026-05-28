package qingzhou.app.demo;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import qingzhou.api.*;
import qingzhou.logger.Logger;

@App(icon = "Shop",
        name = {"示例应用", "en:Demo Application"},
        info = {"用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "User", order = 1)
@Menu(name = {"高级功能", "en:Advanced"}, code = "advanced", icon = "Tools", order = 2)
@Menu(name = {"子菜单一", "en:Sub Menu 1"}, code = "sub1", parent = "advanced", icon = "Document", order = 1)
@Menu(name = {"子菜单二", "en:Sub Menu 2"}, code = "sub2", parent = "advanced", icon = "Folder", order = 2)
@Menu(name = {"三级菜单示例", "en:Level 3 Menu"}, code = "level3", parent = "sub2", icon = "Star", order = 1)
@Menu(name = {"监视", "en:Monitor"}, code = "monitor", icon = "Monitor", order = 3)
@Menu(name = {"系统", "en:System"}, code = "system", icon = "Setting", order = 4)
public class DemoApp implements QingzhouApp {
    @Override
    public boolean available(AppContext appContext) {
        return !new File(appContext.getTemp(), "stop").exists();
    }

    @Override
    public void start(AppContext appContext) throws Exception {
        Logger logger = appContext.getService(Logger.class);
        logger.info("Demo 应用启动成功！");
        logger.info("当前进程 PID: " + appContext.getPid());

        Properties properties = appContext.getProperties();
        logger.info("配置参数：" + properties);
        appContext.addActionFilter((request, chain) -> {
            JvmMonitor jvmMonitor = appContext.getObjectInstance(JvmMonitor.class);
            Map<String, String> monitor = jvmMonitor.monitor(request);
            String heapUsed = monitor.get("heapUsed");
            int i = Integer.parseInt(heapUsed);
            if (i >= 100) {
                logger.warn("内存过大（MB）：" + i);
            }
            chain.doFilter();
        });

        Thread.sleep(2000); // 确保图书管理应用启动完成
        SharedFunction<String, String> testSharedFunction = appContext.getSharedFunction("queryBook");
        String bookInfo = testSharedFunction.invoke("示例应用->调用->图书管理应用共享的方法，查询图书：");
        logger.info(bookInfo);
    }
}
