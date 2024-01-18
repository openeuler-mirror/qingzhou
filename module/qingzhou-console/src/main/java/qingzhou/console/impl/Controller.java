package qingzhou.console.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletImpl;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.FileUtil;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;

public class Controller implements BundleActivator {
    private ProcessSequence sequence;
    private FrameworkContext frameworkContext;
    private ServletService servletService;
    private Logger logger;
    private ServiceReference<FrameworkContext> reference;

    @Override
    public void start(BundleContext context) throws Exception {
        reference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(reference);
        logger = frameworkContext.getService(LoggerService.class).getLogger();

        if (!frameworkContext.isMaster()) return;

        sequence = new ProcessSequence(
                new InstallMasterApp(),
                new StartServlet(),
                new RunWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        if (!frameworkContext.isMaster()) return;

        sequence.undo();
        frameworkContext = null;
        context.ungetService(reference);
    }

    private class InstallMasterApp implements Process {

        @Override
        public void exec() throws Exception {
            logger.info("install master app");
            File masterApp = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "master");
            frameworkContext.getAppManager().installApp(masterApp);
        }
    }

    private class RunWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            File console = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "console");
            String docBase = console.getAbsolutePath();
            contextPath = "/console"; // TODO 需要可配置
            servletService.addWebapp(contextPath, docBase);
            logger.info("Open a browser to access the QingZhou console: http://localhost:9060" + contextPath);// todo 9060 应该动态获取到
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }

    private class StartServlet implements Process {
        @Override
        public void exec() throws Exception {
            ConsoleWarHelper.fc = frameworkContext; //给 tonmcat 部署的 war 内部使用

            servletService = new ServletImpl();
            servletService.start(9060, // TODO 端口需要可以配置
                    frameworkContext.getCache().getAbsolutePath());
        }

        @Override
        public void undo() {
            servletService.stop();
        }
    }
}
