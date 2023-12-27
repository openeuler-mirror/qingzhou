package qingzhou.example.tomcat;

import qingzhou.api.AppContext;
import qingzhou.api.QingZhouApp;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

public class Main implements QingZhouApp {
    @Override
    public void install(AppContext context) {
        System.out.println("安装tomcat程序");
    }

    @Override
    public void start(AppContext context) {
        LoggerService service = context.getService(LoggerService.class);
        Logger logger = service.getLogger();
        logger.info("开始运行tomcat程序");
        logger.info("所在轻舟节点：" + context.getDomain());
        logger.info("轻舟节点的服务列表：");
        for (Class<?> serviceType : context.getServiceTypes()) {
            logger.info(serviceType.getName());
        }
        logger.info("轻舟节点的临时目录：" + context.getTemp());
    }

    @Override
    public void stop(AppContext context) {
        System.out.println("停止tomcat运行");
    }

    @Override
    public void uninstall(AppContext context) {
        System.out.println("tomcat程序正在卸载，此方法执行后，应用包将会被删除");
    }
}
