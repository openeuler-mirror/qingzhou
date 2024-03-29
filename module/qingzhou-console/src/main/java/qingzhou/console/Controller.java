package qingzhou.console;

import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.ModuleLoader;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletServiceImpl;
import qingzhou.framework.config.Config;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.pattern.Process;
import qingzhou.framework.util.pattern.ProcessSequence;

import java.io.File;

public class Controller implements ModuleLoader {
    public static FrameworkContext framework;
    public static Logger logger;
    private static Config config;

    private ProcessSequence sequence;
    private ServletService servletService;
    private int consolePort;

    @Override
    public void start(FrameworkContext context) throws Exception {
        Controller.framework = context;

        if (!framework.isMaster()) {
            return;
        }
        logger = context.getServiceManager().getService(Logger.class);
        config = context.getServiceManager().getService(Config.class);

        sequence = new ProcessSequence(
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(FrameworkContext context) {
        if (sequence != null) {
            sequence.undo();
        }
    }

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            servletService = new ServletServiceImpl();
            consolePort = Integer.parseInt(config.getConfig("//console").get("port"));
            servletService.start(consolePort,
                    framework.getTemp("servlet-container").getAbsolutePath());
        }

        @Override
        public void undo() {
            servletService.stop();
        }
    }

    private class DeployWar implements Process {
        private String contextPath;

        @Override
        public void exec() {
            File consoleApp = FileUtil.newFile(framework.getLib(), "module", "qingzhou-console", "console");
            String docBase = consoleApp.getAbsolutePath();
            contextPath = config.getConfig("//console").get("contextRoot");
            servletService.addWebapp(contextPath, docBase);
            framework.getServiceManager().getService(Logger.class).info("Open a browser to access the Qingzhou console: http://localhost:" + consolePort + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
