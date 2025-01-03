package qingzhou.core.deployer.impl;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.AppListener;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.QingzhouSystemApp;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.Registry;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.logger.Logger;

public class Controller implements Process {
    private final ModuleContext moduleContext;
    private DeployerImpl deployer;
    private ProcessSequence sequence;

    public Controller(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void exec() throws Throwable {
        sequence = new ProcessSequence(
                new InitDeployer(),
                new RegisterService(),
                new InjectShareableAddonsForApp(),
                new ServiceHelper(moduleContext),
                new StartLocalApp()
        );
        sequence.exec();
    }

    @Override
    public void undo() {
        sequence.undo();
    }

    private class InitDeployer implements Process {

        @Override
        public void exec() {
            deployer = new DeployerImpl(moduleContext, moduleContext.getService(Registry.class));
            deployer.appsBase = FileUtil.newFile(moduleContext.getInstanceDir(), "apps");

            deployer.addAppListener(new AppListener() {
                @Override
                public void onAppStarted(String appName) {
                    if (DeployerConstants.APP_SYSTEM.equals(appName)) {
                        deployer.removeAppListener(this);
                        disableSysActions();
                    }
                }
            });
        }

        private void disableSysActions() {
            Map<String, String> registry = (Map<String, String>) ((Map<String, Object>) moduleContext.getConfig()).get("registry");
            boolean disableMaster = registry == null || !Boolean.parseBoolean(registry.get("enabled"));
            if (disableMaster) {
                AppInfo sysApp = deployer.getAppInfo(DeployerConstants.APP_SYSTEM);
                sysApp.removeModelInfo(sysApp.getModelInfo(DeployerConstants.MODEL_MASTER));
            }
        }
    }

    private class RegisterService implements Process {
        @Override
        public void exec() {
            moduleContext.registerService(Deployer.class, deployer);
            moduleContext.registerService(ActionInvoker.class, new ActionInvokerImpl(moduleContext));
        }
    }

    private class InjectShareableAddonsForApp implements Process {
        @Override
        public void exec() throws Exception {
            File addonsDir = FileUtil.newFile(moduleContext.getLibDir(), "addons"); //保持一致：qingzhou.engine.impl.ModuleLoading.BuildModuleInfo
            File[] addonFiles = addonsDir.listFiles();
            if (addonFiles == null || addonFiles.length == 0) return;

            Map<Class<?>, Object> injectedServices = HackUtil.getInjectedServices(moduleContext);
            ServiceHelper.injectedServicesBackupForCore.addAll(injectedServices.keySet());

            try {
                ClassLoader apiLoader = moduleContext.getApiLoader();
                Collection<String> annotatedClasses = Utils.detectAnnotatedClass(
                        addonFiles, Service.class, apiLoader);
                for (String detectedService : annotatedClasses) {
                    Class<?> serviceClass = apiLoader.loadClass(detectedService);
                    Object registeredService = findRegisteredService(serviceClass);
                    if (registeredService != null) {
                        injectedServices.put(serviceClass, registeredService);
                    }
                }
            } catch (Throwable e) {
                moduleContext.getService(Logger.class).warn(e.getMessage(), e);
            }
        }

        private Object findRegisteredService(Class<?> serviceClass) throws Throwable {
            final Object[] service = new Object[1];
            HackUtil.visitAllModuleContext(moduleContext, moduleContext -> {
                try {
                    service[0] = HackUtil.getRegisteredServices(moduleContext).get(serviceClass);
                } catch (Exception ignored) {
                }
                return service[0] == null;
            });
            return service[0];
        }
    }

    private class StartLocalApp implements Process {
        @Override
        public void exec() {
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
            File systemApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-core", DeployerConstants.APP_SYSTEM);
            doStartApp(systemApp);

            deployer.setLoaderPolicy(new DeployerImpl.LoaderPolicy() {
                final File commonApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-core", "common");

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
                    doStartApp(file);
                }
            }
        }

        private void doStartApp(File file) {
            try {
                deployer.reStartApp(file);
            } catch (Throwable e) {
                moduleContext.getService(Logger.class).error("failed to start app " + file.getName(), e);
            }
        }
    }
}
