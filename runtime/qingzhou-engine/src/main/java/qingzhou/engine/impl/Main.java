package qingzhou.engine.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.ServiceRegister;
import qingzhou.engine.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
    private final List<Module> moduleList = new ArrayList<>();
    private final ModuleContext moduleContext = new ModuleContextImpl();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        CompositeClassLoader rootLoader = new CompositeClassLoader(null);
        File apiJarFile = new File(moduleContext.getLibDir(), "qingzhou-api.jar");
        rootLoader.add(new URLClassLoader(new URL[]{apiJarFile.toURI().toURL()}));
        rootLoader.add(Module.class.getClassLoader());

        LinkedHashMap<File, Set<File>> startLevel = startLevel();

        ClassLoader[] moduleLoaders = moduleLoaders(startLevel, rootLoader);

        for (ClassLoader moduleLoader : moduleLoaders) {
            startModule(moduleLoader);
        }

        waitForStop();
    }

    private ClassLoader[] moduleLoaders(LinkedHashMap<File, Set<File>> startLevel, ClassLoader parentLoader) {
        Map<File, ClassLoader> apiLoaders = new HashMap<>();
        startLevel.forEach((key, value) -> {
            apiLoaders.computeIfAbsent(key, file -> {
                try {
                    return splitApiLoader(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            value.forEach(file -> apiLoaders.computeIfAbsent(file, file1 -> {
                try {
                    return splitApiLoader(file1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        });

        ArrayList<Map.Entry<File, Set<File>>> entries = new ArrayList<>(startLevel.entrySet());
        Collections.reverse(entries);
        List<ClassLoader> loaderList = new ArrayList<>();
        entries.forEach(entry -> {
            CompositeClassLoader moduleApi = new CompositeClassLoader(null);
            FileFilterLoader fileApiLoader = (FileFilterLoader) apiLoaders.get(entry.getKey());
            moduleApi.add(fileApiLoader);
            entry.getValue().forEach(file -> moduleApi.add(apiLoaders.get(file)));

            try {
                ClassLoader implLoader = new FileFilterLoader(fileApiLoader.file, name -> !fileApiLoader.filter.accept(name), ); parentLoader or moduleApi 都不行？
                loaderList.add(implLoader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return loaderList.toArray(new ClassLoader[0]);
    }

    private ClassLoader splitApiLoader(File moduleFile) throws IOException {
        String moduleFileName = moduleFile.getName();
        int i = moduleFileName.lastIndexOf(".");
        String moduleName = moduleFileName.substring(0, i);
        String apiPackage = moduleName.replace("-", ".");

        ResourceLoadingFilter apiFilter = name -> {
            if (name.startsWith(apiPackage) && name.endsWith(".class")) {
                String simpleName = name.substring(apiPackage.length() + 1, name.lastIndexOf(".class".length()));
                return !simpleName.contains(".");// 不再有子目录，表示最外层的api
            }
            return false;
        };

        return new FileFilterLoader(moduleFile, apiFilter, null);
    }

    private LinkedHashMap<File, Set<File>> startLevel() throws Exception {
        LinkedHashMap<File, Set<File>> startLevel = new LinkedHashMap<>();

        String moduleConfigFile = "/module.properties";
        try (InputStream inputStream = Main.class.getResourceAsStream(moduleConfigFile)) {
            if (inputStream == null) throw new IllegalStateException("Not found: " + moduleConfigFile);

            Properties properties = FileUtil.streamToProperties(inputStream);
            File moduleDir = new File(moduleContext.getLibDir(), "module");
            properties.stringPropertyNames().forEach(k -> {
                File kJar = new File(moduleDir, k + ".jar");
                if (kJar.isFile()) {
                    String vs = properties.getProperty(k);
                    Set<File> vJars = new HashSet<>();
                    for (String v : vs.split(",")) {
                        File vJar = new File(moduleDir, v + ".jar");
                        if (vJar.isFile()) {
                            vJars.add(vJar);
                        }
                    }
                    startLevel.put(kJar, vJars);
                }
            });
        }

        return startLevel;
    }

    private void startModule(ClassLoader classLoader) {
        try {
            for (Module module : ServiceLoader.load(Module.class, classLoader)) {
                if (module instanceof ServiceRegister) {
                    Class<?> serviceType = ((ServiceRegister<?>) module).serviceType();
                    if (moduleList.stream().anyMatch(module1 -> (module1 instanceof ServiceRegister) && (((ServiceRegister<?>) module1).serviceType() == serviceType))) {
                        continue;
                    }
                }

                module.start(moduleContext);
                moduleList.add(module);

                System.out.println("The module is loaded: " + module.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForStop() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = moduleList.size() - 1; i >= 0; i--) {
                Module module = moduleList.remove(i);
                module.stop();

                System.out.println("The module has been unloaded: " + module.getClass().getName());
            }
        }));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }

    private static class FileFilterLoader extends URLClassLoader {
        final File file;
        final ResourceLoadingFilter filter;

        private final ZipFile zipFile;

        protected FileFilterLoader(File file, ResourceLoadingFilter filter, ClassLoader parent) throws IOException {
            super(new URL[]{file.toURI().toURL()}, parent);
            this.file = file;
            this.filter = filter;
            this.zipFile = new ZipFile(file);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (this.filter.accept(name)) {
                ZipEntry entry = this.zipFile.getEntry(name.replace(".", "/"));
                if (entry != null) {
                    try (InputStream resourceStream = this.zipFile.getInputStream(entry)) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        FileUtil.copyStream(resourceStream, bos);
                        return defineClass(name, bos.toByteArray(), 0, bos.size());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return super.findClass(name);
        }
    }

    private interface ResourceLoadingFilter {
        boolean accept(String name);
    }
}
