package qingzhou.framework.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import qingzhou.crypto.CryptoService;
import qingzhou.framework.AppInfoManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.impl.app.AppContextImpl;
import qingzhou.framework.impl.app.AppInfoImpl;
import qingzhou.framework.impl.app.AppInfoManagerImpl;
import qingzhou.framework.impl.app.ConsoleContextImpl;
import qingzhou.framework.impl.log.SystemPrintStream;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;
import qingzhou.serializer.SerializerService;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Controller implements BundleActivator {
    private ProcessSequence processSequence;
    private FrameworkContextImpl frameworkContext;

    private final Set<ServiceReference<?>> serviceReferences = new HashSet<>();

    @Override
    public void start(BundleContext context) throws Exception {
        processSequence = new ProcessSequence(
                new InitFramework(context),
                new InitLogger(context),
                new ShowInfo(),
                new FindService(context),
                new InitApps()
        );
        processSequence.exec();
    }

    @Override
    public void stop(BundleContext context) {
        processSequence.undo();

        serviceReferences.forEach(context::ungetService);
    }

    private class InitApps implements Process {
        private ProcessSequence sequence;

        @Override
        public void exec() throws Exception {
            sequence = new ProcessSequence(
                    new InitApp(),
                    new InitAppContext(),
                    new StartApp()
            );
            sequence.exec();
        }

        @Override
        public void undo() {
            sequence.undo();
        }

        private class StartApp implements Process {

            @Override
            public void exec() {
                AppInfoManager appInfoManager = frameworkContext.getAppInfoManager();
                appInfoManager.getApps().forEach(s -> {
                    AppInfoImpl appInfo = (AppInfoImpl) appInfoManager.getAppInfo(s);
                    try {
                        appInfo.getQingZhouApp().start();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void undo() {
                AppInfoManager appInfoManager = frameworkContext.getAppInfoManager();
                appInfoManager.getApps().forEach(s -> {
                    AppInfoImpl appInfo = (AppInfoImpl) appInfoManager.getAppInfo(s);
                    try {
                        appInfo.getQingZhouApp().stop();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        private class InitAppContext implements Process {
            @Override
            public void exec() {
                AppInfoManager appInfoManager = frameworkContext.getAppInfoManager();
                appInfoManager.getApps().forEach(s -> {
                    AppInfoImpl appInfo = (AppInfoImpl) appInfoManager.getAppInfo(s);
                    ConsoleContextImpl consoleContext;
                    try {
                        consoleContext = new ConsoleContextImpl(appInfo.getClassLoader());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    AppContextImpl appContext = new AppContextImpl(frameworkContext);
                    appContext.setConsoleContext(consoleContext);
                    appInfo.setAppContext(appContext);
                });
            }
        }

        private class InitApp implements Process {
            @Override
            public void exec() throws Exception {
                AppInfoManagerImpl appInfoManager = (AppInfoManagerImpl) frameworkContext.getAppInfoManager();

                File[] apps = appsDir().listFiles();
                if (apps == null) return;

                for (File app : apps) {
                    if (!app.isDirectory()) continue;
                    String appName = app.getName();
                    if (appName.startsWith(".")) continue;// mac: .DS_Store

                    appInfoManager.addApp(app);
                }
            }

            @Override
            public void undo() {
                AppInfoManager appInfoManager = frameworkContext.getAppInfoManager();
                appInfoManager.getApps().forEach(appInfoManager::removeApp);
            }

            private File appsDir() throws IOException {
                File deployment = new File(ServerUtil.getDomain(), "apps");
                File[] files = deployment.listFiles();
                if (files != null) {
                    String unzipSuffix = ".zip";
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.toLowerCase().endsWith(unzipSuffix)) {
                            File unzipFile = new File(file.getParentFile(), fileName.substring(0, fileName.length() - unzipSuffix.length()));
                            if (!unzipFile.exists()) {
                                ServerUtil.unZipToDir(file, file.getParentFile());
                            }
                        }
                    }
                }
                return deployment;
            }
        }
    }

    private class FindService implements Process {
        private final BundleContext context;

        private FindService(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            Class<?>[] serviceTypes = {SerializerService.class, CryptoService.class};
            for (Class serviceType : serviceTypes) {
                ServiceReference<?> reference = context.getServiceReference(serviceType);
                serviceReferences.add(reference);
                frameworkContext.registerService(serviceType, context.getService(reference));
            }
        }
    }

    private class ShowInfo implements Process {
        private final String[] BANNER = {"",
                "                 |~",
                "           |/    w",
                "          / (   (|   \\",
                "         /( (/   |)  |\\",
                "  ____  ( (/    (|   | )  ,",
                " |----\\ (/ |    /|   |'\\ /^;",
                "\\---*---Y--+-----+---+--/(",
                " \\------*---*--*---*--/",
                "  '~~ ~~~~~~~~~~~~~~~"};
        private Logger logger;

        @Override
        public void exec() {
            logger = frameworkContext.getService(LoggerService.class).getLogger();

            for (String line : BANNER) {
                logger.info(line);
            }
            // 打印版本信息
            logger.info("");
            logger.info("    Powered by QingZhou");
            logger.info("");

            // takeoverSystemPrint();
        }

        private void takeoverSystemPrint() {
            System.setOut(new SystemPrintStream(false, logger));
            System.setErr(new SystemPrintStream(true, logger));
        }
    }

    private class InitLogger implements Process {
        private final BundleContext context;

        private InitLogger(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            ServiceReference<LoggerService> reference = context.getServiceReference(LoggerService.class);
            serviceReferences.add(reference);
            LoggerService loggerService = context.getService(reference);
            frameworkContext.registerService(LoggerService.class, loggerService);
        }
    }

    private class InitFramework implements Process {
        private final BundleContext context;
        private ServiceRegistration<FrameworkContext> registration;

        private InitFramework(BundleContext context) {
            this.context = context;
        }

        @Override
        public void exec() {
            frameworkContext = new FrameworkContextImpl();
            registration = context.registerService(FrameworkContext.class, frameworkContext, null);

            FrameworkContextImpl.instance = frameworkContext; // TODO 若不是 I18n 需要，应该移除这个设计
        }

        @Override
        public void undo() {
            registration.unregister();
            frameworkContext = null;
        }
    }
}
