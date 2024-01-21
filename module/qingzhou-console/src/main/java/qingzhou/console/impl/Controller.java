package qingzhou.console.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletServiceImpl;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.FileUtil;

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
        logger = frameworkContext.getLogger();

        if (!frameworkContext.isMaster()) return;

        sequence = new ProcessSequence(
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        context.ungetService(reference);
        if (!frameworkContext.isMaster()) return;

        sequence.undo();
    }

    private class DeployWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            File console = FileUtil.newFile(frameworkContext.getFileManager().getLib(), "sysapp", "console");
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

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            ConsoleWarHelper.fc = frameworkContext; //给 tonmcat 部署的 war 内部使用

            servletService = new ServletServiceImpl();
            servletService.start(9060, // TODO 端口需要可以配置
                    frameworkContext.getFileManager().getCache().getAbsolutePath());
        }

        @Override
        public void undo() {
            servletService.stop();

            ConsoleWarHelper.fc = null;
        }
    }
}
