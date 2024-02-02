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
    private ServiceReference<FrameworkContext> reference;
    private int consolePort;

    @Override
    public void start(BundleContext context) throws Exception {
        reference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(reference);

        if (!Boolean.parseBoolean(frameworkContext.getConfigManager().getConfig("//console").get("enabled"))) return;

        sequence = new ProcessSequence(
                new InstallMasterApp(),
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        context.ungetService(reference);

        if (sequence != null) {
            sequence.undo();
        }
    }

    private class InstallMasterApp implements Process {
        @Override
        public void exec() throws Exception {
            File masterApp = FileUtil.newFile(frameworkContext.getFileManager().getLib(), "sysapp", FrameworkContext.SYS_APP_MASTER);
            frameworkContext.getAppManager().installApp(masterApp);
        }
    }

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            ConsoleWarHelper.fc = frameworkContext; //给 tonmcat 部署的 war 内部使用

            servletService = new ServletServiceImpl();
            consolePort = Integer.parseInt(frameworkContext.getConfigManager().getConfig("//console").get("port"));
            servletService.start(consolePort,
                    frameworkContext.getFileManager().getTemp("servlet-container").getAbsolutePath());
        }

        @Override
        public void undo() {
            servletService.stop();

            ConsoleWarHelper.fc = null;
        }
    }

    private class DeployWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            File console = FileUtil.newFile(frameworkContext.getFileManager().getLib(), "sysapp", "console");
            String docBase = console.getAbsolutePath();
            contextPath = frameworkContext.getConfigManager().getConfig("//console").get("contextRoot");
            servletService.addWebapp(contextPath, docBase);
            Logger logger = frameworkContext.getServiceManager().getService(Logger.class);
            logger.info("Open a browser to access the Qingzhou console: http://localhost:" + consolePort + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
