package qingzhou.engine.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.Resource;
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
    private final List<ModuleInfo> moduleStartedOrderCache = new ArrayList<>();
    private final ProcessSequence sequence;

    public ModuleLoading(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.sequence = new ProcessSequence(
                new BuildModuleInfo(),
                new BuildModuleLoader(),
                new BuildModuleActivator(),
                new SetModule(),
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
            File[] pluginFiles = new File(engineContext.getLibDir(), "addons").listFiles(); // 保持一致 ：assembly.xml 里的 addons
            if (pluginFiles != null) {
                moduleFiles.addAll(Arrays.asList(pluginFiles));
            }

            for (File moduleFile : moduleFiles) {
                String fileName = moduleFile.getName();
                if (fileName.startsWith("qingzhou-") && fileName.endsWith(".jar")) {
                    engineContext.moduleInfoList.add(new ModuleInfo(moduleFile, engineContext));
                }
            }
        }

        @Override
        public void undo() {
            engineContext.moduleInfoList.clear();
        }
    }

    private class BuildModuleLoader implements Process {

        @Override
        public void exec() throws Exception {
            URLClassLoader parentLoader = new URLClassLoader(
                    new URL[]{new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                    Main.class.getClassLoader());
            new FilterLoading().setModuleLoader(engineContext.moduleInfoList, parentLoader);
        }

        @Override
        public void undo() {
            engineContext.moduleInfoList.forEach(moduleInfo -> {
                try {
                    moduleInfo.getLoader().close();
                } catch (IOException e) {
                    String msg = Utils.exceptionToString(e);
                    System.err.println(msg);
                }
            });
        }
    }

    private class SetModule implements Process {

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

            for (ModuleInfo moduleInfo : engineContext.moduleInfoList) {
                Object c = qzJson.get(moduleInfo.getName());
                if (c != null) {
                    moduleInfo.moduleContext.config = c;
                }
            }
        }
    }

    private class BuildModuleActivator implements Process {

        @Override
        public void exec() throws Exception {
            for (ModuleInfo moduleInfo : engineContext.moduleInfoList) {
                Collection<String> annotatedClasses = Utils.detectAnnotatedClass(
                        new File[]{moduleInfo.getFile()},
                        Module.class, moduleInfo.getLoader());
                for (String a : annotatedClasses) {
                    Class<?> moduleClass = moduleInfo.getLoader().loadClass(a);
                    ModuleActivator activator = (ModuleActivator) moduleClass.newInstance();
                    moduleInfo.moduleActivators.add(activator);
                }
            }
        }

        @Override
        public void undo() {
            engineContext.moduleInfoList.forEach(moduleInfo -> moduleInfo.moduleActivators.clear());
        }
    }

    private class StartModule implements Process {

        @Override
        public void exec() throws Exception {
            Collection<ModuleInfo> toStartList = engineContext.moduleInfoList;
            while (true) {
                Map<ModuleInfo, Set<Class<?>>> missingServiceModule = startModule(toStartList);
                if (missingServiceModule.isEmpty()) break; // 已经全部启动完毕

                if (missingServiceModule.size() == toStartList.size()) {
                    StringBuilder error = new StringBuilder();
                    missingServiceModule.forEach((key, value) -> error
                            .append(key.getName())
                            .append(" fails to start, missing Services: ")
                            .append(Arrays.toString(value.toArray())).append(System.lineSeparator()));
                    throw new IllegalStateException(error.toString());
                }

                toStartList = missingServiceModule.keySet();
            }
        }

        @Override
        public void undo() {
            Collections.reverse(moduleStartedOrderCache); // Qingzhou Logger module must be the last one to stop.
            moduleStartedOrderCache.forEach(moduleInfo -> {
                moduleInfo.setStarted(false);
                moduleInfo.moduleActivators.forEach(ModuleActivator::stop);
            });
            moduleStartedOrderCache.clear();
        }

        Map<ModuleInfo, Set<Class<?>>> startModule(Collection<ModuleInfo> toStartList) throws Exception {
            Map<ModuleInfo, Set<Class<?>>> missingServiceModule = new HashMap<>();
            for (ModuleInfo moduleInfo : toStartList) {
                Set<Class<?>> missing = injectRequiredService(moduleInfo);
                if (missing.isEmpty()) {
                    for (ModuleActivator moduleActivator : moduleInfo.moduleActivators) {
                        moduleActivator.start(moduleInfo.moduleContext);
                    }
                    moduleInfo.setStarted(true);
                    moduleStartedOrderCache.add(moduleInfo);
                } else {
                    missingServiceModule.computeIfAbsent(moduleInfo, a -> new HashSet<>()).addAll(missing);
                }
            }
            return missingServiceModule;
        }

        Set<Class<?>> injectRequiredService(ModuleInfo moduleInfo) throws Exception {
            Set<Class<?>> missingServices = new HashSet<>();
            for (ModuleActivator moduleActivator : moduleInfo.moduleActivators) {
                Field[] fields;
                try {
                    fields = moduleActivator.getClass().getDeclaredFields();
                } catch (Throwable e) {
                    System.err.println("module getDeclaredFields error, " + moduleInfo.getName() + ": " + e.getMessage());
                    throw e;
                }
                for (Field field : fields) {
                    Resource resource = field.getAnnotation(Resource.class);
                    if (resource == null) continue;

                    Class<?> serviceType = field.getType();
                    Object serviceObj = findService(serviceType);
                    if (serviceObj == null) {
                        if (!resource.optional()) {
                            missingServices.add(serviceType);
                        }

                        continue;
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

            return missingServices;
        }

        Object findService(Class<?> serviceType) {
            final Object[] serviceObj = {null};
            engineContext.moduleInfoList.stream().filter(ModuleInfo::isStarted).forEach(moduleInfo -> {
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
