package qingzhou.engine.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Main {
    private final EngineContext engineContext = new EngineContext();
    private final Map<String, ModuleInfo> moduleInfoMap = new HashMap<>();
    private final List<String> moduleOrder = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        parseModuleFile();

        buildModuleLoader();

        loadModuleActivator();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> moduleOrder.forEach(moduleName -> {
            ModuleInfo moduleInfo = moduleInfoMap.get(moduleName);
            moduleInfo.getModuleActivators().forEach(ModuleActivator::stop);
            try {
                FileUtil.forceDelete(moduleInfo.getModuleContext().getTemp());
            } catch (IOException ignored) {
            }
        })));

        startModule();

        synchronized (Main.class) {
            Main.class.wait();
        }
    }

    private void startModule() throws Exception {
        for (String s : moduleOrder) {
            ModuleInfo moduleInfo = moduleInfoMap.get(s);
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

    private void loadModuleActivator() throws Exception {
        for (String s : moduleOrder) {
            ModuleInfo moduleInfo = moduleInfoMap.get(s);
            Collection<String> annotatedClasses = FileUtil.detectAnnotatedClass(
                    new File[]{moduleInfo.getFile()},
                    Module.class,
                    "qingzhou.");
            for (String a : annotatedClasses) {
                Class<?> aClass = moduleInfo.getLoader().loadClass(a);
                ModuleActivator activator = (ModuleActivator) aClass.newInstance();
                moduleInfo.getModuleActivators().add(activator);
            }
        }
    }

    private void injectModuleService(ModuleInfo moduleInfo) throws Exception {
        for (ModuleActivator moduleActivator : moduleInfo.getModuleActivators()) {
            for (Field field : moduleActivator.getClass().getDeclaredFields()) {
                Service service = field.getAnnotation(Service.class);
                if (service != null) {
                    Object serviceObj = findService(field.getType(), moduleInfo.getDependencies());
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
    }

    private static Object findService(Class<?> serviceType, Set<ModuleInfo> dependencies) {
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
        if (serviceObj == null) {
            throw new IllegalStateException("Service not found");
        }
        return serviceObj;
    }

    private void buildModuleLoader() throws Exception {
        File tempBase = new File(engineContext.getTemp(), "qz-lib-cache");
        String moduleApiFileName = "module-api.jar";
        String moduleImplSuffixName = "-impl.jar";
        splitFiles(tempBase, moduleApiFileName, moduleImplSuffixName); // todo 优化：免去文件操作，从lib下动态加载

        URLClassLoader appApiLoader = new URLClassLoader(new URL[]
                {new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                Main.class.getClassLoader());

        URLClassLoader moduleApiLoader = new URLClassLoader(new URL[]{new File(tempBase, moduleApiFileName).toURI().toURL()}, appApiLoader);

        moduleOrder.forEach(s -> {
            File file = new File(tempBase, s + moduleImplSuffixName);
            try {
                URLClassLoader implLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, moduleApiLoader);
                moduleInfoMap.get(s).setLoader(implLoader);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void splitFiles(File tempBase, String apiFileName, String implSuffix) throws Exception {
        FileUtil.mkdirs(tempBase);
        FileUtil.cleanDirectory(tempBase);
        // 合并 api jar
        Set<String> alreadyNames = new HashSet<>();
        try (ZipOutputFile apiJar = new ZipOutputFile(new File(tempBase, apiFileName))) {
            for (String moduleName : moduleOrder) {
                File moduleFile = moduleInfoMap.get(moduleName).getFile();
                try (ZipInputStream moduleZip = new ZipInputStream(Files.newInputStream(moduleFile.toPath()))) {
                    File moduleImplFile = new File(tempBase, moduleName + implSuffix);
                    try (ZipOutputFile moduleImplJar = new ZipOutputFile(moduleImplFile)) {
                        String apiPkg = moduleName.replace("-", "/") + "/";
                        for (ZipEntry zipEntry; (zipEntry = moduleZip.getNextEntry()) != null; ) {
                            try {
                                String entryName = zipEntry.getName();

                                if (zipEntry.isDirectory()) {
                                    moduleImplJar.writeZipEntry(entryName, true, null);
                                    if (apiPkg.startsWith(entryName)) {
                                        if (alreadyNames.add(entryName)) { // 多个 jar 都有 qingzhou/ 这个根目录，多次会重复报错
                                            apiJar.writeZipEntry(entryName, true, null);
                                        }
                                    }
                                } else {
                                    boolean isApiResource = false;
                                    if (entryName.startsWith(apiPkg)) {
                                        int inner = entryName.indexOf("/", apiPkg.length());
                                        if (inner == -1) { // 不需要子目录
                                            // 写入 api jar
                                            apiJar.writeZipEntry(entryName, false, moduleZip);
                                            isApiResource = true;
                                        }
                                    }
                                    // 其它文件，都切割到 impl jar
                                    if (!isApiResource) {
                                        moduleImplJar.writeZipEntry(entryName, false, moduleZip);
                                    }
                                }
                            } finally {
                                moduleZip.closeEntry();
                            }
                        }
                    }
                }
            }
        }
    }

    private static class ZipOutputFile implements AutoCloseable {
        final ZipOutputStream zos;

        private ZipOutputFile(File file) throws IOException {
            this.zos = new ZipOutputStream(Files.newOutputStream(file.toPath()));
        }

        void writeZipEntry(String entryName, boolean isDirectory, InputStream entryStream) throws IOException {
            this.zos.putNextEntry(new ZipEntry(entryName));

            if (!isDirectory) {
                FileUtil.copyStream(entryStream, zos);
            }

            this.zos.closeEntry();
        }

        @Override
        public void close() throws Exception {
            this.zos.close();
        }
    }

    private void parseModuleFile() throws Exception {
        String moduleConfigFile = "/module.properties";
        try (InputStream inputStream = Main.class.getResourceAsStream(moduleConfigFile)) {
            if (inputStream == null) throw new IllegalStateException("Not found: " + moduleConfigFile);

            LinkedHashMap<String, String> data = FileUtil.streamToProperties(inputStream);
            File moduleDir = new File(engineContext.getLibDir(), "module");
            data.forEach((key, vs) -> {
                File kJar = new File(moduleDir, key + ".jar");
                if (!kJar.isFile()) return;

                moduleOrder.add(key);
                ModuleInfo keyModule = moduleInfoMap.computeIfAbsent(key, s -> new ModuleInfo(key));
                keyModule.setFile(kJar);

                for (String v : vs.split(",")) {
                    File vJar = new File(moduleDir, v + ".jar");
                    if (!vJar.isFile()) continue;

                    ModuleInfo vModule = moduleInfoMap.computeIfAbsent(v, s -> new ModuleInfo(v));
                    vModule.setFile(vJar);

                    keyModule.getDependencies().add(vModule);
                }
            });
        }

        Collections.reverse(moduleOrder);
    }
}
