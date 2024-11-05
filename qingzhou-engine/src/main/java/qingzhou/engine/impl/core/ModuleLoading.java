package qingzhou.engine.impl.core;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.Service;
import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.impl.Main;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ModuleLoading implements Process {
    private final EngineContext engineContext;
    private final List<ModuleInfo> moduleInfoList = new ArrayList<>();
    private final List<ModuleInfo> moduleStartedOrder = new ArrayList<>();
    private final ProcessSequence sequence;

    public ModuleLoading(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.sequence = new ProcessSequence(
                new BuildModuleInfo(),
                new BuildModuleLoader(),
                new BuildModuleActivator(),
                new SetModuleConfig(),
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
            List<File> moduleFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(moduleDir.listFiles())));
            File[] pluginFiles = new File(engineContext.getLibDir(), "plugins").listFiles();
            if (pluginFiles != null) {
                moduleFiles.addAll(Arrays.asList(pluginFiles));
            }

            for (File moduleFile : moduleFiles) {
                String fileName = moduleFile.getName();
                if (fileName.startsWith("qingzhou-") && fileName.endsWith(".jar")) {
                    moduleInfoList.add(new ModuleInfo(moduleFile, engineContext));
                }
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
            URLClassLoader parentLoader = new URLClassLoader(
                    new URL[]{new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                    Main.class.getClassLoader());
            new FilterLoading().setModuleLoader(moduleInfoList, parentLoader);
        }

        @Override
        public void undo() {
            moduleInfoList.forEach(moduleInfo -> {
                try {
                    moduleInfo.getLoader().close();
                } catch (IOException e) {
                    String msg = Utils.exceptionToString(e);
                    System.err.println(msg);
                }
            });
        }
    }

    private class SetModuleConfig implements Process {

        @Override
        public void exec() throws Exception {
            Map<String, ?> qzJson;
            URL jsonUrl = Paths.get(engineContext.getLibDir().getAbsolutePath(), "module", "qingzhou-json.jar").toUri().toURL();
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jsonUrl})) {
                Class<?> loadedClass = classLoader.loadClass("qingzhou.json.impl.JsonImpl");
                Object instance = loadedClass.newInstance();
                Method fromJson = loadedClass.getMethod("fromJson", Reader.class, Class.class, String[].class);
                try (InputStream inputStream = Files.newInputStream(
                        new File(new File(engineContext.getInstanceDir(), "conf"), "qingzhou.json").toPath())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    qzJson = (Map<String, ?>) fromJson.invoke(instance, reader, Map.class, new String[]{"module"});
                }
            }

            for (ModuleInfo moduleInfo : moduleInfoList) {
                Object c = qzJson.get(moduleInfo.getName());
                if (c != null) {
                    moduleInfo.moduleContext.config = new HashMap<>((Map<String, String>) c);
                }
            }
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
                    Class<?> moduleClass = moduleInfo.getLoader().loadClass(a);
                    ModuleActivator activator = (ModuleActivator) moduleClass.newInstance();
                    moduleInfo.moduleActivators.add(activator);
                }
            }
        }

        @Override
        public void undo() {
            moduleInfoList.forEach(moduleInfo -> moduleInfo.moduleActivators.clear());
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
                    missingServiceModule.forEach((key, value) -> error
                            .append(key.getName())
                            .append(" fails to start, missing Service: ")
                            .append(value).append(System.lineSeparator()));
                    throw new IllegalStateException(error.toString());
                }

                toStartList = missingServiceModule.keySet();
            }
        }

        @Override
        public void undo() {
            Collections.reverse(moduleStartedOrder); // Qingzhou Logger module must be the last one to stop.
            moduleStartedOrder.forEach(moduleInfo -> {
                moduleInfo.setStarted(false);
                moduleInfo.moduleActivators.forEach(ModuleActivator::stop);
            });
            moduleStartedOrder.clear();
        }

        Map<ModuleInfo, Class<?>> startModule(Collection<ModuleInfo> toStartList) throws Exception {
            Map<ModuleInfo, Class<?>> missingServiceModule = new HashMap<>();
            for (ModuleInfo moduleInfo : toStartList) {
                Class<?> missing = injectRequiredService(moduleInfo);
                if (missing == null) {
                    for (ModuleActivator moduleActivator : moduleInfo.moduleActivators) {
                        moduleActivator.start(moduleInfo.moduleContext);
                    }
                    moduleInfo.setStarted(true);
                    moduleStartedOrder.add(moduleInfo);
                } else {
                    missingServiceModule.put(moduleInfo, missing);
                }
            }
            return missingServiceModule;
        }

        Class<?> injectRequiredService(ModuleInfo moduleInfo) throws Exception {
            for (ModuleActivator moduleActivator : moduleInfo.moduleActivators) {
                Field[] fields;
                try {
                    fields = moduleActivator.getClass().getDeclaredFields();
                } catch (Throwable e) {
                    System.err.println("module getDeclaredFields error, " + moduleInfo.getName() + ": " + e.getMessage());
                    throw e;
                }
                for (Field field : fields) {
                    Service service = field.getAnnotation(Service.class);
                    if (service == null) continue;

                    Class<?> serviceType = field.getType();
                    Object serviceObj = findService(serviceType);
                    if (serviceObj == null) {
                        return service.optional() ? null : serviceType;
                    }

                    field.setAccessible(true);
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) {
                        field.set(null, serviceObj);
                    } else {
                        field.set(moduleActivator, serviceObj);
                    }

                    moduleInfo.moduleContext.injectedServices.put(serviceType, serviceObj);
                }
            }

            return null;
        }

        Object findService(Class<?> serviceType) {
            final Object[] serviceObj = {null};
            moduleInfoList.stream().filter(ModuleInfo::isStarted).forEach(moduleInfo -> {
                Map<Class<?>, Object> registeredServices = moduleInfo.moduleContext.registeredServices;
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
