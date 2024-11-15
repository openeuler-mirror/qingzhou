package qingzhou.deployer.impl;

import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.*;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.Registry;
import qingzhou.serializer.Serializer;
import qingzhou.servlet.ServletService;
import qingzhou.uml.Uml;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Module
public class Controller implements ModuleActivator {
    @Resource
    static Logger logger;

    @Resource
    private Registry registry;

    @Resource
    private Config config;

    @Resource
    private Json json;

    @Resource
    private Serializer serializer;

    @Resource
    private QrGenerator qrGenerator;

    @Resource
    private Http http;

    @Resource
    private ServletService servletService;

    @Resource
    private CryptoService cryptoService;

    @Resource
    private Uml uml; // 引用类：qingzhou.deployer.impl.SuperAction.combined

    private DeployerImpl deployer;
    private ProcessSequence sequence;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        sequence = new ProcessSequence(
                new RegisterService(moduleContext),
                new InjectShareablePluginsForApp(moduleContext),
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
            deployer = new DeployerImpl(moduleContext, registry);
            deployer.appsBase = FileUtil.newFile(moduleContext.getInstanceDir(), "apps");

            moduleContext.registerService(Deployer.class, deployer);
            moduleContext.registerService(ActionInvoker.class, new ActionInvokerImpl(deployer, registry, json, cryptoService, http, config));
        }
    }

    private static class InjectShareablePluginsForApp implements Process {
        final ModuleContext moduleContext;

        InjectShareablePluginsForApp(ModuleContext moduleContext) {
            this.moduleContext = moduleContext;
        }

        @Override
        public void exec() throws Exception {
            File pluginsDir = FileUtil.newFile(moduleContext.getLibDir(), "plugins");
            File[] listFiles = pluginsDir.listFiles();
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
            } catch (Exception e) {
                Controller.logger.warn(e.getMessage(), e);
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
