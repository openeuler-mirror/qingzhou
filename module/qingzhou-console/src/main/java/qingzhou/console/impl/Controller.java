package qingzhou.console.impl;

import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.bootstrap.main.ModuleLoader;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletServiceImpl;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.serializer.Serializer;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.pattern.Process;
import qingzhou.framework.util.pattern.ProcessSequence;

import java.io.File;

public class Controller implements ModuleLoader {
    public static FrameworkContext framework;
    public static Config config;
    public static Logger logger;
    public static AppManager appManager;
    public static Serializer serializer;
    public static CryptoService cryptoService;

    private ProcessSequence sequence;
    private ServletService servletService;
    private int consolePort;

    @Override
    public void start(FrameworkContext context) throws Exception {
        Controller.framework = context;
        config = context.getServiceManager().getService(Config.class);
        logger = context.getServiceManager().getService(Logger.class);
        appManager = context.getServiceManager().getService(AppManager.class);
        serializer = context.getServiceManager().getService(Serializer.class);
        cryptoService = context.getServiceManager().getService(CryptoService.class);

        if (!Boolean.parseBoolean(config.getConfig("//console").get("enabled")))
            return;

        sequence = new ProcessSequence(
                new InstallMasterApp(),
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

    private static class InstallMasterApp implements Process {
        @Override
        public void exec() throws Exception {
            File masterApp = FileUtil.newFile(framework.getLib(), "module", "qingzhou-app", App.SYS_APP_MASTER);
            appManager.installApp(masterApp);
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
            logger.info("Open a browser to access the Qingzhou console: http://localhost:" + consolePort + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
