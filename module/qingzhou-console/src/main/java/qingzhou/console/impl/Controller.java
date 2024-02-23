package qingzhou.console.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.app.AppManager;
import qingzhou.config.ConfigManager;
import qingzhou.console.servlet.ServletService;
import qingzhou.console.servlet.impl.ServletServiceImpl;
import qingzhou.crypto.CryptoService;
import qingzhou.framework.Framework;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.framework.util.FileUtil;
import qingzhou.logger.Logger;
import qingzhou.serializer.Serializer;

import java.io.File;

public class Controller implements BundleActivator {
    private ServiceReference<Framework> frameworkContextReference;
    private ServiceReference<ConfigManager> configManagerReference;
    private ServiceReference<Logger> loggerReference;
    private ServiceReference<AppManager> appManagerReference;
    private ServiceReference<Serializer> serializerReference;
    private ServiceReference<CryptoService> cryptoServiceReference;

    public static Framework framework;
    public static ConfigManager configManager;
    public static Logger logger;
    public static AppManager appManager;
    public static Serializer serializer;
    public static CryptoService cryptoService;

    private ProcessSequence sequence;
    private ServletService servletService;
    private int consolePort;

    @Override
    public void start(BundleContext context) throws Exception {
        frameworkContextReference = context.getServiceReference(Framework.class);
        configManagerReference = context.getServiceReference(ConfigManager.class);
        loggerReference = context.getServiceReference(Logger.class);
        appManagerReference = context.getServiceReference(AppManager.class);
        serializerReference = context.getServiceReference(Serializer.class);
        cryptoServiceReference = context.getServiceReference(CryptoService.class);

        framework = context.getService(frameworkContextReference);
        configManager = context.getService(configManagerReference);
        logger = context.getService(loggerReference);
        appManager = context.getService(appManagerReference);
        serializer = context.getService(serializerReference);
        cryptoService = context.getService(cryptoServiceReference);

        if (!Boolean.parseBoolean(configManager.getConfig("//console").get("enabled")))
            return;

        sequence = new ProcessSequence(
                new InstallMasterApp(),
                new StartServletContainer(),
                new DeployWar()
        );
        sequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        context.ungetService(cryptoServiceReference);
        context.ungetService(serializerReference);
        context.ungetService(appManagerReference);
        context.ungetService(loggerReference);
        context.ungetService(configManagerReference);
        context.ungetService(frameworkContextReference);

        if (sequence != null) {
            sequence.undo();
        }
    }

    private class InstallMasterApp implements Process {
        @Override
        public void exec() throws Exception {
            File masterApp = FileUtil.newFile(framework.getLib(), "module", "qingzhou-app", "master");
            appManager.installApp(masterApp);
        }
    }

    private class StartServletContainer implements Process {
        @Override
        public void exec() throws Exception {
            servletService = new ServletServiceImpl();
            consolePort = Integer.parseInt(configManager.getConfig("//console").get("port"));
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
            contextPath = configManager.getConfig("//console").get("contextRoot");
            servletService.addWebapp(contextPath, docBase);
            logger.info("Open a browser to access the Qingzhou console: http://localhost:" + consolePort + contextPath);
        }

        @Override
        public void undo() {
            servletService.removeApp(contextPath);
        }
    }
}
