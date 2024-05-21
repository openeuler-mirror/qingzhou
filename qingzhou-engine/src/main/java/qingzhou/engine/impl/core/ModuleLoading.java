package qingzhou.engine.impl.core;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.Service;
import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.impl.Main;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ModuleLoading implements Process {
    private final EngineContext engineContext;
    private final List<ModuleInfo> moduleInfoList = new ArrayList<>();
    private final ProcessSequence sequence;

    public ModuleLoading(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.sequence = new ProcessSequence(
                new BuildModuleInfo(),
                new BuildModuleLoader(),
                new BuildModuleActivator(),
                new StartModule()
        );
    }

    @Override
    public void exec() throws Exception {
        sequence.exec();
    }

    @Override
    public void undo() {
        sequence.undo();
    }

    private class BuildModuleInfo implements Process {

        @Override
        public void exec() {
            File moduleDir = new File(engineContext.getLibDir(), "module");
            File[] moduleFiles = moduleDir.listFiles();
            if (moduleFiles == null) throw new IllegalStateException("Module Directory Not Found: " + moduleDir);

            for (File moduleFile : moduleFiles) {
                ModuleInfo moduleInfo = new ModuleInfo(moduleFile);
                ModuleContextImpl moduleContext = new ModuleContextImpl(moduleInfo.getName(), engineContext);
                moduleInfo.setModuleContext(moduleContext);
                moduleInfoList.add(moduleInfo);
            }
        }

        @Override
        public void undo() {
            moduleInfoList.clear();
        }
    }

    private class BuildModuleLoader implements Process {

        @Override
        public void exec() throws Exception {
            URLClassLoader parentLoader = new URLClassLoader(new URL[]
                    {new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                    Main.class.getClassLoader());
            new FilterLoading().setModuleLoader(moduleInfoList, parentLoader);
        }

        @Override
        public void undo() {
            moduleInfoList.forEach(moduleInfo -> {
                try {
                    moduleInfo.getLoader().close();
                } catch (IOException e) {
                    String msg = Utils.stackTraceToString(e.getStackTrace());
                    System.err.println(msg);
                }
            });
        }
    }

    private class BuildModuleActivator implements Process {

        @Override
        public void exec() throws Exception {
            for (ModuleInfo moduleInfo : moduleInfoList) {
                Collection<String> annotatedClasses = Utils.detectAnnotatedClass(
                        new File[]{moduleInfo.getFile()},
                        Module.class, "qingzhou.", moduleInfo.getLoader());
                for (String a : annotatedClasses) {
                    Class<?> aClass = moduleInfo.getLoader().loadClass(a);
                    ModuleActivator activator = (ModuleActivator) aClass.newInstance();
                    moduleInfo.getModuleActivators().add(activator);
                }
            }
        }

        @Override
        public void undo() {
            moduleInfoList.forEach(moduleInfo -> moduleInfo.getModuleActivators().clear());
        }
    }

    private class StartModule implements Process {

        @Override
        public void exec() throws Exception {
            Collection<ModuleInfo> toStartList = moduleInfoList;
            while (true) {
                Map<ModuleInfo, Class<?>> missingServiceModule = startModule(toStartList);
                if (missingServiceModule.isEmpty()) break; // 已经全部启动完毕

                if (missingServiceModule.size() == toStartList.size()) {
                    StringBuilder error = new StringBuilder();
                    missingServiceModule.forEach((key, value) -> error.append("Service [").append(key).append("] fails to start because it is missing Service: ").append(value).append(System.lineSeparator()));
                    throw new IllegalStateException(error.toString());
                }

                toStartList = missingServiceModule.keySet();
            }
        }

        @Override
        public void undo() {
            moduleInfoList.forEach(moduleInfo -> {
                moduleInfo.setStarted(false);
                moduleInfo.getModuleActivators().forEach(ModuleActivator::stop);
            });
        }

        Map<ModuleInfo, Class<?>> startModule(Collection<ModuleInfo> toStartList) throws Exception {
            Map<ModuleInfo, Class<?>> missingServiceModule = new HashMap<>();
            for (ModuleInfo moduleInfo : toStartList) {
                Class<?> missing = injectRequiredService(moduleInfo);
                if (missing == null) {
                    for (ModuleActivator moduleActivator : moduleInfo.getModuleActivators()) {
                        moduleActivator.start(moduleInfo.getModuleContext());
                    }
                    moduleInfo.setStarted(true);
                } else {
                    missingServiceModule.put(moduleInfo, missing);
                }
            }
            return missingServiceModule;
        }

        Class<?> injectRequiredService(ModuleInfo moduleInfo) throws Exception {
            for (ModuleActivator moduleActivator : moduleInfo.getModuleActivators()) {
                for (Field field : moduleActivator.getClass().getDeclaredFields()) {
                    Service service = field.getAnnotation(Service.class);
                    if (service == null) continue;

                    Class<?> serviceType = field.getType();
                    Object serviceObj = findService(serviceType);
                    if (serviceObj == null) {
                        return serviceType;
                    }

                    field.setAccessible(true);
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) {
                        field.set(null, serviceObj);
                    } else {
                        field.set(moduleActivator, serviceObj);
                    }

                    moduleInfo.getModuleContext().injectedServices.put(serviceType, serviceObj);
                }
            }

            return null;
        }

        Object findService(Class<?> serviceType) {
            final Object[] serviceObj = {null};
            moduleInfoList.stream().filter(ModuleInfo::isStarted).forEach(moduleInfo -> {
                Map<Class<?>, Object> registeredServices = moduleInfo.getModuleContext().registeredServices;
                Object foundService = registeredServices.get(serviceType);
                if (foundService != null) {
                    if (serviceObj[0] == null) {
                        serviceObj[0] = foundService;
                    } else {
                        throw new IllegalStateException("Ambiguous service was found: " + serviceType.getName());
                    }
                }
            });
            return serviceObj[0];
        }
    }
}
