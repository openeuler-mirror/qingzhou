package qingzhou.console.impl;

import qingzhou.config.Config;
import qingzhou.config.ConfigService;
import qingzhou.config.Console;
import qingzhou.console.impl.servlet.ServletService;
import qingzhou.console.impl.servlet.impl.ServletServiceImpl;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.logger.Logger;

import java.io.File;

public class Controller implements Module {
    public static ModuleContext moduleContext;
    public static Logger logger;
    private static Console console;

    private ProcessSequence sequence;
    private ServletService servletService;

    @Override
    public void start(ModuleContext context) throws Exception {
        Controller.moduleContext = context;

        logger = context.getService(Logger.class);
        Config config = context.getService(ConfigService.class).getConfig();
        console = config.getConsole();
        if (!console.isEnabled()) return;

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
            servletService.start(console.getPort(),
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
            contextPath = console.getContextRoot();
            servletService.addWebapp(contextPath, docBase);
            moduleContext.getService(Logger.class).info("Open a browser to access the Qingzhou console: http://localhost:" + console.getPort() + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
