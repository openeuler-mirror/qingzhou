package qingzhou.core.deployer.impl;

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

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
                new InstallApp()
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
                public void onInstalled(String appName) {
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
            File[] listFiles = addonsDir.listFiles();
            if (listFiles == null || listFiles.length == 0) return;

            // hack into ModuleContextImpl:
            // 因底层服务管理模块约束，未经过 @Resource 注入的服务无法通过 getService(xx) 使用，因此，
            // 为了使得应用可以依赖到插件里自定义的服务，强行修改底层服务管理的注册表
            Field field = moduleContext.getClass().getDeclaredField("injectedServices");
            field.setAccessible(true);
            Map<Class<?>, Object> injectedServices = (Map<Class<?>, Object>) field.get(moduleContext);

            try {
                ClassLoader apiLoader = moduleContext.getApiLoader();
                Collection<String> annotatedClasses = Utils.detectAnnotatedClass(
                        listFiles, Service.class, apiLoader);
                for (String detectedService : annotatedClasses) {
                    Class<?> serviceClass = apiLoader.loadClass(detectedService);
                    Object service = getService(serviceClass);
                    if (service != null) {
                        injectedServices.put(serviceClass, service);
                    }
                }
            } catch (Throwable e) {
                moduleContext.getService(Logger.class).warn(e.getMessage(), e);
            }
        }

        private Object getService(Class<?> serviceClass) throws Exception {
            List moduleInfoList = (List) getAllModuleInfos();
            Field moduleContextField = null;
            for (Object otherModuleInfo : moduleInfoList) {
                if (moduleContextField == null) {
                    moduleContextField = otherModuleInfo.getClass().getDeclaredField("moduleContext");
                    moduleContextField.setAccessible(true);
                }
                ModuleContext context = (ModuleContext) moduleContextField.get(otherModuleInfo);
                Object service = context.getService(serviceClass);
                if (service != null) return service;
            }
            return null;
        }

        private Object getAllModuleInfos() throws NoSuchFieldException, IllegalAccessException {
            Field fieldModuleInfo = moduleContext.getClass().getDeclaredField("moduleInfo");
            fieldModuleInfo.setAccessible(true);
            Object moduleInfo = fieldModuleInfo.get(moduleContext);

            Field engineContextField = moduleInfo.getClass().getDeclaredField("engineContext");
            engineContextField.setAccessible(true);
            Object engineContext = engineContextField.get(moduleInfo);

            Field moduleInfoListField = engineContext.getClass().getDeclaredField("moduleInfoList");
            moduleInfoListField.setAccessible(true);
            return moduleInfoListField.get(engineContext);
        }
    }

    private class InstallApp implements Process {
        @Override
        public void exec() throws Throwable {
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
            deployer.installApp(systemApp);

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
                    try {
                        deployer.installApp(file);
                    } catch (Throwable e) {
                        moduleContext.getService(Logger.class).error("failed to install app " + file.getName(), e);
                    }
                }
            }
        }

        @Override
        public void undo() {
            String[] apps = deployer.getLocalApps().toArray(new String[0]);
            Arrays.stream(apps).forEach(appName -> {
                try {
                    deployer.unInstallApp(appName);
                } catch (Exception e) {
                    moduleContext.getService(Logger.class).warn("failed to stop app: " + appName, e);
                }
            });
        }
    }
}
