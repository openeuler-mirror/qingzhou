package qingzhou.app.demo;

import java.util.Map;
import java.util.Properties;

import qingzhou.api.*;
import qingzhou.logger.Logger;

@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "User", order = 1)
@Menu(name = {"高级功能", "en:Advanced"}, code = "advanced", icon = "Tools", order = 2)
@Menu(name = {"子菜单一", "en:Sub Menu 1"}, code = "sub1", parent = "advanced", icon = "Document", order = 1)
@Menu(name = {"子菜单二", "en:Sub Menu 2"}, code = "sub2", parent = "advanced", icon = "Folder", order = 2)
@Menu(name = {"三级菜单示例", "en:Level 3 Menu"}, code = "level3", parent = "sub2", icon = "Star", order = 1)
@Menu(name = {"监视", "en:Monitor"}, code = "monitor", icon = "Monitor", order = 3)
@Menu(name = {"系统", "en:System"}, code = "system", icon = "Setting", order = 4)

@I18n(name = {"基本信息", "en:Basic Info"}, code = "basic")
@I18n(name = {"学术信息", "en:Academic Info"}, code = "academic")
@I18n(name = {"企业信息", "en:Company Info"}, code = "company")
@I18n(name = {"CPU", "en:CPU"}, code = "cpu")
@I18n(name = {"内存", "en:Memory"}, code = "memory")
@I18n(name = {"交换空间", "en:Swap"}, code = "swap")
@I18n(name = {"文件描述符", "en:File Descriptors"}, code = "file")
@I18n(name = {"联系信息", "en:Contact Info"}, code = "Contact")
@I18n(name = {"企业介绍", "en:About"}, code = "About")
@I18n(name = {"配送信息", "en:Delivery"}, code = "Delivery")
@I18n(name = {"订单详情", "en:Order Details"}, code = "Order")
@I18n(name = {"线程", "en:Thread"}, code = "Thread")
@I18n(name = {"类加载", "en:Class Loading"}, code = "Class")
@I18n(name = {"GC", "en:GC"}, code = "GC")
@I18n(name = {"文件信息", "en:File Info"}, code = "FileInfo")
@I18n(name = {"库存与价格", "en:Inventory & Price"}, code = "Inventory")
@I18n(name = {"工作信息", "en:Work Info"}, code = "Work")
@I18n(name = {"系统设置", "en:System Settings"}, code = "System")
@I18n(name = {"通知设置", "en:Notification Settings"}, code = "Notification")
@I18n(name = {"联系方式", "en:Contact"}, code = "Contact")
@I18n(name = {"个人信息", "en:Personal Info"}, code = "Personal")
@I18n(name = {"业务数据", "en:Business Data"}, code = "Business")
@I18n(name = {"教师性别分布", "en:Teacher Gender Distribution"}, code = "TeacherGender")
@I18n(name = {"教师职称分布", "en:Teacher Title Distribution"}, code = "TeacherTitle")
@I18n(name = {"教师启用状态", "en:Teacher Status"}, code = "TeacherStatus")
@I18n(name = {"配置详情", "en:Config Details"}, code = "ConfigDetails")

@App(icon = "DataBoard",
        name = {"示例应用", "en:Demo Application"},
        info = {"用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
public class DemoApp implements QingzhouApp {
    @Override
    public void start(AppContext appContext) throws Exception {
        Logger logger = appContext.getService(Logger.class);
        logger.info("Demo 应用启动成功！");
        logger.info("当前进程 PID: " + appContext.getPid());

        String detectionPath = appContext.getDetectedPath();
        logger.info("探测到的 Java 安装路径：" + detectionPath);

        Properties properties = appContext.getProperties();
        logger.info("配置参数：" + properties);
        appContext.addActionFilter((request, chain) -> {
            JvmMonitor jvmMonitor = appContext.getObjectInstance(JvmMonitor.class);
            Map<String, String> monitor = jvmMonitor.monitor(request.getId());
            String heapUsed = monitor.get("heapUsed");
            int i = Integer.parseInt(heapUsed);
            if (i >= 100) {
                logger.warn("内存过大（MB）：" + i);
            }
            chain.doFilter();
        });

        Thread.sleep(2000); // 确保图书管理应用启动完成
        SharedFunction<String, String> testSharedFunction = appContext.getSharedFunction("queryBook");
        if (testSharedFunction != null) {
            String bookInfo = testSharedFunction.invoke("示例应用->调用->图书管理应用共享的方法，查询图书：");
            logger.info(bookInfo);
        } else {
            logger.warn("未找到共享函数 queryBook，跳过图书查询。请确认图书管理应用已启动并注册了该共享函数。");
        }
    }
}
