package qingzhou.console;

import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletServiceImpl;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.framework.config.Config;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.pattern.Process;
import qingzhou.framework.util.pattern.ProcessSequence;

import java.io.File;

public class Controller implements Module {
    public static ModuleContext moduleContext;
    public static Logger logger;
    private static Config config;

    private ProcessSequence sequence;
    private ServletService servletService;
    private int consolePort;

    @Override
    public void start(ModuleContext context) throws Exception {
        Controller.moduleContext = context;

        logger = context.getService(Logger.class);
        config = context.getService(Config.class);

        sequence = new ProcessSequence(
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop() {
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
                    moduleContext.getTemp().getAbsolutePath());
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
            File consoleApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-console", "console");
            String docBase = consoleApp.getAbsolutePath();
            contextPath = config.getConfig("//console").get("contextRoot");
            servletService.addWebapp(contextPath, docBase);
            moduleContext.getService(Logger.class).info("Open a browser to access the Qingzhou console: http://localhost:" + consolePort + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
