package qingzhou.deployer.impl;

import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.Registry;
import qingzhou.servlet.ServletService;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

@Module
public class Controller implements ModuleActivator {
    @Resource
    private Logger logger;

    @Resource
    private Registry registry;

    @Resource
    private Config config;

    @Resource
    private Json json;

    @Resource
    private QrGenerator qrGenerator;

    @Resource
    private Http http;

    @Resource
    private ServletService servletService;

    @Resource
    private CryptoService cryptoService;

    private DeployerImpl deployer;
    private ProcessSequence sequence;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        sequence = new ProcessSequence(
                new RegisterService(moduleContext),
                new InjectPluginsForApp(moduleContext),
                new InstallApp(moduleContext)
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        sequence.undo();
    }

    private class RegisterService implements Process {
        final ModuleContext moduleContext;

        RegisterService(ModuleContext moduleContext) {
            this.moduleContext = moduleContext;
        }

        @Override
        public void exec() {
            deployer = new DeployerImpl(moduleContext, registry, logger);
            deployer.appsBase = FileUtil.newFile(moduleContext.getInstanceDir(), "apps");

            moduleContext.registerService(Deployer.class, deployer);
            moduleContext.registerService(ActionInvoker.class, new ActionInvokerImpl(deployer, registry, json, cryptoService, http, logger, config));
        }
    }

    private static class InjectPluginsForApp implements Process {
        final ModuleContext moduleContext;

        InjectPluginsForApp(ModuleContext moduleContext) {
            this.moduleContext = moduleContext;
        }

        @Override
        public void exec() throws Exception {
            // hack into ModuleContextImpl
            Field field = moduleContext.getClass().getDeclaredField("injectedServices");
            field.setAccessible(true);
            Map<Class<?>, Object> injectedServices = (Map<Class<?>, Object>) field.get(moduleContext);
        }
    }

    private class InstallApp implements Process {
        final ModuleContext moduleContext;

        private InstallApp(ModuleContext moduleContext) {
            this.moduleContext = moduleContext;
        }

        @Override
        public void exec() throws Exception {
            deployer.setLoaderPolicy(new DeployerImpl.LoaderPolicy() {
                @Override
                public ClassLoader getClassLoader() {
                    return QingzhouSystemApp.class.getClassLoader();
                }

                @Override
                public File[] getAdditionalLib() {
                    return null;
                }
            });
            File systemApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", DeployerConstants.APP_SYSTEM);
            deployer.installApp(systemApp);

            deployer.setLoaderPolicy(new DeployerImpl.LoaderPolicy() {
                final File commonApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common");

                @Override
                public ClassLoader getClassLoader() {
                    return moduleContext.getApiLoader();
                }

                @Override
                public File[] getAdditionalLib() {
                    return commonApp.listFiles();
                }
            });
            File[] files = deployer.appsBase.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isDirectory()) continue;
                    try {
                        deployer.installApp(file);
                    } catch (Exception e) {
                        logger.error("failed to install app " + file.getName(), e);
                    }
                }
            }
        }

        @Override
        public void undo() {
            String[] apps = deployer.getAllApp().toArray(new String[0]);
            Arrays.stream(apps).forEach(appName -> {
                try {
                    deployer.unInstallApp(appName);
                } catch (Exception e) {
                    logger.warn("failed to stop app: " + appName, e);
                }
            });
        }
    }
}
