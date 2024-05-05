package qingzhou.engine.impl.core;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.Service;
import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.impl.Main;
import qingzhou.engine.impl.core.loader.FilterLoading;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

public class Core implements Process {
    private final EngineContext engineContext;

    private final List<ModuleInfo> moduleInfoList = new ArrayList<>();

    public Core(EngineContext engineContext) {
        this.engineContext = engineContext;
    }

    @Override
    public void exec() throws Exception {
        // 清理上次启动的缓存文件
        Utils.forceDelete(engineContext.getTemp());

        parseModuleFile();

//        ModuleLoaderBuilder builder = new SplitFile();
        ModuleLoaderBuilder builder = new FilterLoading();
        builder.build(moduleInfoList, engineContext);

        buildModuleActivators();

        startModule();
    }

    @Override
    public void undo() {
        moduleInfoList.forEach(moduleInfo -> moduleInfo.getModuleActivators().forEach(ModuleActivator::stop));
    }

    private void startModule() throws Exception {
        for (ModuleInfo moduleInfo : moduleInfoList) {
            if (!moduleInfo.getDependencies().isEmpty()) {
                injectModuleService(moduleInfo);
            }
            ModuleContextImpl moduleContext = new ModuleContextImpl(
                    moduleInfo.getName(), engineContext);
            moduleInfo.setModuleContext(moduleContext);
            for (ModuleActivator moduleActivator : moduleInfo.getModuleActivators()) {
                moduleActivator.start(moduleContext);
            }
        }
    }

    private void injectModuleService(ModuleInfo moduleInfo) throws Exception {
        for (ModuleActivator moduleActivator : moduleInfo.getModuleActivators()) {
            for (Field field : moduleActivator.getClass().getDeclaredFields()) {
                Service service = field.getAnnotation(Service.class);
                if (service == null) continue;

                Object serviceObj = findService(field.getType(), moduleInfo.getDependencies());
                if (serviceObj == null) {
                    new Exception("Service " + field.getType().getName() + " required by module " + moduleInfo.getName() + " is not found in the dependency").printStackTrace();
                    continue;
                }

                field.setAccessible(true);
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    field.set(null, serviceObj);
                } else {
                    field.set(moduleActivator, serviceObj);
                }
            }
        }
    }

    private Object findService(Class<?> serviceType, Set<ModuleInfo> dependencies) {
        Object serviceObj = null;
        for (ModuleInfo dependency : dependencies) {
            for (Map.Entry<Class<?>, Object> entry : ((ModuleContextImpl) dependency.getModuleContext()).registeredServices.entrySet()) {
                if (entry.getKey().equals(serviceType)) {
                    if (serviceObj == null) {
                        serviceObj = entry.getValue();
                    } else {
                        throw new IllegalStateException("Ambiguous service was found: " + serviceType.getName());
                    }
                }
            }
        }
        return serviceObj;
    }

    private void buildModuleActivators() throws Exception {
        for (ModuleInfo moduleInfo : moduleInfoList) {
            Collection<String> annotatedClasses = Utils.detectAnnotatedClass(
                    new File[]{moduleInfo.getFile()},
                    Module.class, "qingzhou.");
            for (String a : annotatedClasses) {
                Class<?> aClass = moduleInfo.getLoader().loadClass(a);
                ModuleActivator activator = (ModuleActivator) aClass.newInstance();
                moduleInfo.getModuleActivators().add(activator);
            }
        }
    }

    private void parseModuleFile() throws Exception {
        String moduleConfigFile = "/module.properties";
        try (InputStream inputStream = Main.class.getResourceAsStream(moduleConfigFile)) {
            if (inputStream == null) throw new IllegalStateException("Not found: " + moduleConfigFile);

            LinkedHashMap<String, String> data = Utils.streamToProperties(inputStream);
            ArrayList<Map.Entry<String, String>> entries = new ArrayList<>(data.entrySet());
            Collections.reverse(entries);

            File moduleDir = new File(engineContext.getLibDir(), "module");
            entries.forEach(entry -> {
                String key = entry.getKey();
                String vs = entry.getValue();
                {
                    File kJar = new File(moduleDir, key + ".jar");
                    if (!kJar.isFile()) return;

                    ModuleInfo keyModule = new ModuleInfo(key);
                    keyModule.setFile(kJar);
                    for (String v : vs.split(",")) {
                        File vJar = new File(moduleDir, v + ".jar");
                        if (!vJar.isFile()) continue;

                        try {
                            ModuleInfo vModule = moduleInfoList.stream().filter(moduleInfo -> moduleInfo.getName().equals(v))
                                    .findAny().orElseThrow((Supplier<Throwable>) () ->
                                            new IllegalStateException("Module " + v + " required by " + key + " was not found"));
                            keyModule.getDependencies().add(vModule);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }

                    }
                    moduleInfoList.add(keyModule);
                }
            });
        }
    }
}
